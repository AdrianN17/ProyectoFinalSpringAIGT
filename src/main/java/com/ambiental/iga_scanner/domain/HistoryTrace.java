package com.ambiental.iga_scanner.domain;

import java.time.Instant;
import java.util.UUID;

public record HistoryTrace(
        UUID traceId,
        UUID sessionId,
        String question,
        String answer,
        String user,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt,
        String retrievedContexts) {

    // Static factory: every trace gets a fresh id/timestamp, so callers never build these by hand.
    public static HistoryTrace of(UUID sessionId, String question, String answer, String user,
            Integer inputTokens, Integer outputTokens, String retrievedContexts) {
        return new HistoryTrace(UUID.randomUUID(), sessionId, question, answer, user, inputTokens, outputTokens,
                Instant.now(), retrievedContexts);
    }
}
