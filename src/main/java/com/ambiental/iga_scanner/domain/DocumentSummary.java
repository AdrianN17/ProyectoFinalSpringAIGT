package com.ambiental.iga_scanner.domain;

import java.time.Instant;
import java.util.UUID;

/** Catalog entry for an ingested PDF: identifier, original file name and ingestion timestamp. */
public record DocumentSummary(UUID documentId, String fileName, Instant createdAt) {
}
