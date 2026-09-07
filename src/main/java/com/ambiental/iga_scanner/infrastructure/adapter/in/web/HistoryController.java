package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

import com.ambiental.iga_scanner.application.port.in.GetSessionHistoryUseCase;
import com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto.HistoryEntryDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HistoryController {

    private final GetSessionHistoryUseCase getSessionHistoryUseCase;

    HistoryController(GetSessionHistoryUseCase getSessionHistoryUseCase) {
        this.getSessionHistoryUseCase = getSessionHistoryUseCase;
    }

    @GetMapping("/api/history/{session_id}")
    List<HistoryEntryDto> getHistory(@PathVariable("session_id") UUID sessionId) {
        return getSessionHistoryUseCase.getHistory(sessionId).stream().map(HistoryEntryDto::from).toList();
    }
}
