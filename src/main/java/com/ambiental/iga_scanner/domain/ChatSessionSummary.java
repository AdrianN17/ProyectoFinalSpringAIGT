package com.ambiental.iga_scanner.domain;

import java.time.Instant;
import java.util.UUID;

/** Aggregate view of a chat session: its id, when it was last used, and how many turns it has. */
public record ChatSessionSummary(UUID sessionId, Instant lastActivityAt, long messageCount) {
}
