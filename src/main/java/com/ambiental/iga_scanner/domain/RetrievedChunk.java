package com.ambiental.iga_scanner.domain;

/** A single vector-store hit: verbatim source text plus the document it was cited from. */
public record RetrievedChunk(String content, String sourceDocument, Integer page) {
}
