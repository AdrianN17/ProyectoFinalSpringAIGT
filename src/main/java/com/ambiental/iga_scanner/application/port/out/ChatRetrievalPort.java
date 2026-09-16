package com.ambiental.iga_scanner.application.port.out;

import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface ChatRetrievalPort {

    /**
     * @param documentIds when non-empty, restricts retrieval to chunks belonging to these
     *                    document ids only; when null or empty, searches the whole knowledge base.
     */
    List<RetrievedChunk> retrieve(String query, int topK, List<UUID> documentIds);
}
