package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.ai.QdrantChatRetrievalAdapterMessageError.RETRIEVAL_FAILED;

import com.ambiental.iga_scanner.application.port.out.ChatRetrievalPort;
import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
class QdrantChatRetrievalAdapter implements ChatRetrievalPort {

    static final String SOURCE_METADATA_KEY = "source";
    static final String PAGE_METADATA_KEY = "page";

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    QdrantChatRetrievalAdapter(VectorStore vectorStore, RagRetrievalProperties properties) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = properties.similarityThreshold();
    }

    @Override
    public List<RetrievedChunk> retrieve(String query, int topK) {
        // Guardrail: only weakly-related noise is discarded here; chunks below this score
        // never reach the model, so it can't extrapolate a hallucinated answer from a poor match.
        try {
            var results = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build());
            return results.stream()
                    .map(document -> new RetrievedChunk(
                            document.getText(),
                            String.valueOf(document.getMetadata().getOrDefault(SOURCE_METADATA_KEY, "desconocido")),
                            (Integer) document.getMetadata().get(PAGE_METADATA_KEY)))
                    .toList();
        } catch (RuntimeException e) {
            throw new RetrievalFailedException(RETRIEVAL_FAILED, e);
        }
    }
}
