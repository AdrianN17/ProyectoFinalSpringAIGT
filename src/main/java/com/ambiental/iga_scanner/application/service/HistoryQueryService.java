package com.ambiental.iga_scanner.application.service;

import com.ambiental.iga_scanner.application.port.in.GetSessionHistoryUseCase;
import com.ambiental.iga_scanner.application.port.out.HistoryRepositoryPort;
import com.ambiental.iga_scanner.domain.HistoryTrace;
import java.util.List;
import java.util.UUID;

public class HistoryQueryService implements GetSessionHistoryUseCase {

    private final HistoryRepositoryPort historyRepositoryPort;

    public HistoryQueryService(HistoryRepositoryPort historyRepositoryPort) {
        this.historyRepositoryPort = historyRepositoryPort;
    }

    @Override
    public List<HistoryTrace> getHistory(UUID sessionId) {
        return historyRepositoryPort.findBySessionId(sessionId);
    }
}
