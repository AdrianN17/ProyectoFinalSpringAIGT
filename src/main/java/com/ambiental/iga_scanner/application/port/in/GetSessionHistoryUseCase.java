package com.ambiental.iga_scanner.application.port.in;

import com.ambiental.iga_scanner.domain.HistoryTrace;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface GetSessionHistoryUseCase {
    List<HistoryTrace> getHistory(UUID sessionId);
}
