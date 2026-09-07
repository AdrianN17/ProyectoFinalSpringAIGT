package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HistoryJpaRepository extends JpaRepository<HistoryEntryEntity, UUID> {
    List<HistoryEntryEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
