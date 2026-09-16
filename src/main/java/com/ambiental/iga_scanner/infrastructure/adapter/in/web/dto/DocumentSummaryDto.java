package com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto;

import com.ambiental.iga_scanner.domain.DocumentSummary;
import java.time.Instant;
import java.util.UUID;

public record DocumentSummaryDto(UUID documentId, String fileName, Instant createdAt) {

    public static DocumentSummaryDto from(DocumentSummary summary) {
        return new DocumentSummaryDto(summary.documentId(), summary.fileName(), summary.createdAt());
    }
}
