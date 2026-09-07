package com.ambiental.iga_scanner.application.service;

import com.ambiental.iga_scanner.application.port.in.UploadPdfCommand;
import com.ambiental.iga_scanner.application.port.in.UploadPdfUseCase;
import com.ambiental.iga_scanner.application.port.out.DocumentIngestionPort;
import com.ambiental.iga_scanner.domain.IngestedDocument;

public class PdfUploadService implements UploadPdfUseCase {

    private final DocumentIngestionPort documentIngestionPort;

    public PdfUploadService(DocumentIngestionPort documentIngestionPort) {
        this.documentIngestionPort = documentIngestionPort;
    }

    @Override
    public IngestedDocument upload(UploadPdfCommand command) {
        return documentIngestionPort.ingest(command.fileName(), command.content());
    }
}
