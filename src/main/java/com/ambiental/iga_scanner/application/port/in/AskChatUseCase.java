package com.ambiental.iga_scanner.application.port.in;

import com.ambiental.iga_scanner.domain.HistoryTrace;

@FunctionalInterface
public interface AskChatUseCase {
    HistoryTrace ask(ChatCommand command);
}
