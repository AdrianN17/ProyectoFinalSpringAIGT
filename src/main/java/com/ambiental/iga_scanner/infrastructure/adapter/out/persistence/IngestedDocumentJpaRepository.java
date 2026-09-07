package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface IngestedDocumentJpaRepository extends JpaRepository<IngestedDocumentEntity, UUID> {
}
