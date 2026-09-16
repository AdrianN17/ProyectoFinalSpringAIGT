package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import static com.ambiental.iga_scanner.infrastructure.adapter.out.persistence.DocumentCatalogRepositoryAdapterMessageError.LIST_FAILED;
import static com.ambiental.iga_scanner.infrastructure.adapter.out.persistence.DocumentCatalogRepositoryAdapterMessageError.RECORD_FAILED;

import com.ambiental.iga_scanner.application.port.out.DocumentCatalogPort;
import com.ambiental.iga_scanner.domain.DocumentSummary;
import com.ambiental.iga_scanner.domain.IngestedDocument;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
class DocumentCatalogRepositoryAdapter implements DocumentCatalogPort {

    private final IngestedDocumentJpaRepository jpaRepository;

    DocumentCatalogRepositoryAdapter(IngestedDocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<String> listDocumentNames() {
        try {
            return jpaRepository.findAll().stream()
                    .map(IngestedDocumentEntity::getFileName)
                    .distinct()
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceException(LIST_FAILED, e);
        }
    }

    @Override
    public List<DocumentSummary> listDocuments() {
        try {
            return jpaRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .map(entity -> new DocumentSummary(
                            entity.getDocumentId(), entity.getFileName(), entity.getCreatedAt()))
                    .toList();
        } catch (DataAccessException e) {
            throw new PersistenceException(LIST_FAILED, e);
        }
    }

    @Override
    public void recordUpload(IngestedDocument document) {
        try {
            jpaRepository.save(new IngestedDocumentEntity(
                    document.documentId(),
                    document.fileName(),
                    document.chunkCount(),
                    document.status().name(),
                    Instant.now()));
        } catch (DataAccessException e) {
            throw new PersistenceException(RECORD_FAILED, e);
        }
    }
}
