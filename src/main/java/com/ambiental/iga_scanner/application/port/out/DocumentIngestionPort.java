package com.ambiental.iga_scanner.application.port.out;

import com.ambiental.iga_scanner.domain.IngestedDocument;

@FunctionalInterface
public interface DocumentIngestionPort {
    IngestedDocument ingest(String fileName, byte[] pdfBytes);
}
