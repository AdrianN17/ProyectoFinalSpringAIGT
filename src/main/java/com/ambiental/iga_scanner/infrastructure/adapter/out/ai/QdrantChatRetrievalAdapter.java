package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.QdrantChatRetrievalAdapterMessageError.RETRIEVAL_FAILED;

import com.ambiental.iga_scanner.application.port.out.ChatRetrievalPort;
import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
class QdrantChatRetrievalAdapter implements ChatRetrievalPort {

    private static final Logger log = LoggerFactory.getLogger(QdrantChatRetrievalAdapter.class);

    static final String SOURCE_METADATA_KEY = "source";
    static final String PAGE_METADATA_KEY = "page";
    static final String DOCUMENT_ID_METADATA_KEY = "document_id";

    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final double maxScoreGap;
    private final FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

    QdrantChatRetrievalAdapter(VectorStore vectorStore, RagRetrievalProperties properties) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = properties.similarityThreshold();
        this.maxScoreGap = properties.maxScoreGap();
    }

    @Override
    public List<RetrievedChunk> retrieve(String query, int topK, List<UUID> documentIds) {
        // Guardrail: only weakly-related noise is discarded here; chunks below this score
        // never reach the model, so it can't extrapolate a hallucinated answer from a poor match.
        try {
            Filter.Expression documentFilter = buildDocumentFilter(documentIds);

            var resultsAboveThreshold = vectorStore.similaritySearch(applyFilter(SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold), documentFilter)
                    .build());

            if (resultsAboveThreshold.isEmpty()) {
                // Re-run without the threshold purely for diagnostics: lets us see in the logs
                // whether Qdrant matched anything at all and how far below the cutoff it was,
                // instead of silently returning "no context found" with no further clues.
                var unfilteredResults = vectorStore.similaritySearch(applyFilter(SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.0), documentFilter)
                        .build());
                log.warn("No chunks passed similarity_threshold={} for query='{}' (topK={}, documents={}). "
                                + "Top {} unfiltered matches (score, source, preview): {}",
                        similarityThreshold, query, topK, documentIds, unfilteredResults.size(),
                        describeForLog(unfilteredResults));
                return List.of();
            }

            // Absolute similarity thresholds alone don't discriminate well with this
            // embedding model: every chunk from the same source document tends to cluster
            // in a narrow score band for any question about that document, so a fixed cutoff
            // either drops the right answer or lets in most of the document. Relative
            // filtering fixes this per-query: only keep chunks within maxScoreGap of the
            // best match found for THIS question, regardless of where the absolute scores land.
            double topScore = resultsAboveThreshold.get(0).getScore();
            var relevantResults = resultsAboveThreshold.stream()
                    .filter(document -> topScore - document.getScore() <= maxScoreGap)
                    .toList();

            log.info("Retrieved {}/{} chunk(s) above similarity_threshold={} within max_score_gap={} of "
                            + "top_score={} for query='{}' (documents={}): {}",
                    relevantResults.size(), resultsAboveThreshold.size(), similarityThreshold, maxScoreGap,
                    topScore, query, documentIds, describeForLog(relevantResults));

            return relevantResults.stream()
                    .map(document -> new RetrievedChunk(
                            document.getText(),
                            String.valueOf(document.getMetadata().getOrDefault(SOURCE_METADATA_KEY, "desconocido")),
                            (Integer) document.getMetadata().get(PAGE_METADATA_KEY)))
                    .toList();
        } catch (RuntimeException e) {
            throw new RetrievalFailedException(RETRIEVAL_FAILED, e);
        }
    }

    // Restricts retrieval to specific document ids (as tagged on every chunk at ingestion
    // time) when the caller asked for it; otherwise searches the whole shared knowledge base.
    private Filter.Expression buildDocumentFilter(List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return null;
        }
        List<Object> ids = documentIds.stream().map(UUID::toString).map(Object.class::cast).toList();
        return filterExpressionBuilder.in(DOCUMENT_ID_METADATA_KEY, ids).build();
    }

    private SearchRequest.Builder applyFilter(SearchRequest.Builder builder, Filter.Expression filter) {
        return filter != null ? builder.filterExpression(filter) : builder;
    }

    private String describeForLog(List<Document> documents) {
        return documents.stream()
                .map(document -> {
                    String text = document.getText();
                    String preview = text == null ? "" : text.substring(0, Math.min(80, text.length())).replace('\n', ' ');
                    return "[score=%s, source=%s, preview='%s...']".formatted(
                            document.getScore(),
                            document.getMetadata().getOrDefault(SOURCE_METADATA_KEY, "desconocido"),
                            preview);
                })
                .toList()
                .toString();
    }
}
