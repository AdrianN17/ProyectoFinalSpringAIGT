package com.ambiental.iga_scanner.application.port.out;

import com.ambiental.iga_scanner.domain.ChatSessionSummary;
import com.ambiental.iga_scanner.domain.HistoryTrace;
import java.util.List;
import java.util.UUID;

public interface HistoryRepositoryPort {
    HistoryTrace save(HistoryTrace trace);

    List<HistoryTrace> findBySessionId(UUID sessionId);

    List<ChatSessionSummary> findAllSessions();
}
