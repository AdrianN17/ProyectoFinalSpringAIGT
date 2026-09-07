package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
class IngestedDocumentEntity {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IngestedDocumentEntity() {
    }

    IngestedDocumentEntity(UUID documentId, String fileName, int chunkCount, String status, Instant createdAt) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.chunkCount = chunkCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    UUID getDocumentId() {
        return documentId;
    }

    String getFileName() {
        return fileName;
    }

    int getChunkCount() {
        return chunkCount;
    }

    String getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IngestedDocumentEntity other)) {
            return false;
        }
        return documentId != null && documentId.equals(other.documentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
