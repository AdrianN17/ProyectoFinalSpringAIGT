package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

/** JPQL constructor-expression projection backing {@link HistoryJpaRepository#findSessionSummaries()}. */
interface SessionAggregateProjection {
    UUID getSessionId();

    Instant getLastActivityAt();

    Long getMessageCount();
}
