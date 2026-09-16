package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.QdrantChatRetrievalAdapterMessageError.RETRIEVAL_FAILED;

import com.ambiental.iga_scanner.application.port.out.ChatRetrievalPort;
import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
class QdrantChatRetrievalAdapter implements ChatRetrievalPort {

    private static final Logger log = LoggerFactory.getLogger(QdrantChatRetrievalAdapter.class);

    static final String SOURCE_METADATA_KEY = "source";
    static final String PAGE_METADATA_KEY = "page";

    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final double maxScoreGap;

    QdrantChatRetrievalAdapter(VectorStore vectorStore, RagRetrievalProperties properties) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = properties.similarityThreshold();
        this.maxScoreGap = properties.maxScoreGap();
    }

    @Override
    public List<RetrievedChunk> retrieve(String query, int topK) {
        // Guardrail: only weakly-related noise is discarded here; chunks below this score
        // never reach the model, so it can't extrapolate a hallucinated answer from a poor match.
        try {
            var resultsAboveThreshold = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build());

            if (resultsAboveThreshold.isEmpty()) {
                // Re-run without the threshold purely for diagnostics: lets us see in the logs
                // whether Qdrant matched anything at all and how far below the cutoff it was,
                // instead of silently returning "no context found" with no further clues.
                var unfilteredResults = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.0)
                        .build());
                log.warn("No chunks passed similarity_threshold={} for query='{}' (topK={}). "
                                + "Top {} unfiltered matches (score, source, preview): {}",
                        similarityThreshold, query, topK, unfilteredResults.size(),
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
                            + "top_score={} for query='{}': {}",
                    relevantResults.size(), resultsAboveThreshold.size(), similarityThreshold, maxScoreGap,
                    topScore, query, describeForLog(relevantResults));

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
