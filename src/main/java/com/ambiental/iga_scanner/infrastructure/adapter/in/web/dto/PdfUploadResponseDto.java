package com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto;

import com.ambiental.iga_scanner.domain.DocumentStatus;
import com.ambiental.iga_scanner.domain.IngestedDocument;
import java.util.UUID;

public record PdfUploadResponseDto(UUID documentId, String fileName, int chunkCount, DocumentStatus status) {

    public static PdfUploadResponseDto from(IngestedDocument document) {
        return new PdfUploadResponseDto(
                document.documentId(), document.fileName(), document.chunkCount(), document.status());
    }
}
