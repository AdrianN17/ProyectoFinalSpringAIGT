package com.ambiental.iga_scanner.application.service;

import com.ambiental.iga_scanner.application.port.in.ListChatSessionsUseCase;
import com.ambiental.iga_scanner.application.port.out.HistoryRepositoryPort;
import com.ambiental.iga_scanner.domain.ChatSessionSummary;
import java.util.List;

public class ChatSessionQueryService implements ListChatSessionsUseCase {

    private final HistoryRepositoryPort historyRepositoryPort;

    public ChatSessionQueryService(HistoryRepositoryPort historyRepositoryPort) {
        this.historyRepositoryPort = historyRepositoryPort;
    }

    @Override
    public List<ChatSessionSummary> listSessions() {
        return historyRepositoryPort.findAllSessions();
    }
}
