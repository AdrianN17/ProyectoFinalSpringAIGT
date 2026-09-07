package com.ambiental.iga_scanner.application.port.out;

import com.ambiental.iga_scanner.domain.RetrievedChunk;
import java.util.List;

@FunctionalInterface
public interface ChatRetrievalPort {
    List<RetrievedChunk> retrieve(String query, int topK);
}
