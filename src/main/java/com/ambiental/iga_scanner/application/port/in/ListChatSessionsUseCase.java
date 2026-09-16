package com.ambiental.iga_scanner.application.port.in;

import com.ambiental.iga_scanner.domain.ChatSessionSummary;
import java.util.List;

@FunctionalInterface
public interface ListChatSessionsUseCase {
    List<ChatSessionSummary> listSessions();
}
