package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface HistoryJpaRepository extends JpaRepository<HistoryEntryEntity, UUID> {
    List<HistoryEntryEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    @Query("SELECT h.sessionId AS sessionId, MAX(h.createdAt) AS lastActivityAt, COUNT(h) AS messageCount "
            + "FROM HistoryEntryEntity h GROUP BY h.sessionId ORDER BY MAX(h.createdAt) DESC")
    List<SessionAggregateProjection> findSessionSummaries();
}
