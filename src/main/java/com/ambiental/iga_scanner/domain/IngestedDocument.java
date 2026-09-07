package com.ambiental.iga_scanner.domain;

import java.util.UUID;

public record IngestedDocument(UUID documentId, String fileName, int chunkCount, DocumentStatus status) {
}
