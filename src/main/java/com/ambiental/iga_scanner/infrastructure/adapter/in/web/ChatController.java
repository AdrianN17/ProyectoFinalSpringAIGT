package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

import com.ambiental.iga_scanner.application.port.in.AskChatUseCase;
import com.ambiental.iga_scanner.application.port.in.ChatCommand;
import com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto.ChatRequestDto;
import com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto.HistoryEntryDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ChatController {

    private final AskChatUseCase askChatUseCase;

    ChatController(AskChatUseCase askChatUseCase) {
        this.askChatUseCase = askChatUseCase;
    }

    @PostMapping("/api/chat")
    HistoryEntryDto ask(@Valid @RequestBody ChatRequestDto request) {
        var trace = askChatUseCase.ask(
                new ChatCommand(request.sessionId(), request.question(), request.user(), request.documents()));
        return HistoryEntryDto.from(trace);
    }
}
