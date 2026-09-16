package com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto;

import com.ambiental.iga_scanner.domain.ChatSessionSummary;
import java.time.Instant;
import java.util.UUID;

public record ChatSessionSummaryDto(UUID sessionId, Instant lastActivityAt, long messageCount) {

    public static ChatSessionSummaryDto from(ChatSessionSummary summary) {
        return new ChatSessionSummaryDto(summary.sessionId(), summary.lastActivityAt(), summary.messageCount());
    }
}
