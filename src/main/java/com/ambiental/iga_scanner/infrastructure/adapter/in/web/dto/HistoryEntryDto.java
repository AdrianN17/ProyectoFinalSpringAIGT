package com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto;

import com.ambiental.iga_scanner.domain.HistoryTrace;
import java.time.Instant;
import java.util.UUID;

public record HistoryEntryDto(
        UUID traceId,
        UUID sessionId,
        String question,
        String answer,
        String user,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt,
        String retrievedContexts) {

    public static HistoryEntryDto from(HistoryTrace trace) {
        return new HistoryEntryDto(
                trace.traceId(),
                trace.sessionId(),
                trace.question(),
                trace.answer(),
                trace.user(),
                trace.inputTokens(),
                trace.outputTokens(),
                trace.createdAt(),
                trace.retrievedContexts());
    }
}
