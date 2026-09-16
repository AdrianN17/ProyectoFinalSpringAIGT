package com.ambiental.iga_scanner.application.service;

import com.ambiental.iga_scanner.application.port.in.ListDocumentsUseCase;
import com.ambiental.iga_scanner.application.port.out.DocumentCatalogPort;
import com.ambiental.iga_scanner.domain.DocumentSummary;
import java.util.List;

public class DocumentCatalogQueryService implements ListDocumentsUseCase {

    private final DocumentCatalogPort documentCatalogPort;

    public DocumentCatalogQueryService(DocumentCatalogPort documentCatalogPort) {
        this.documentCatalogPort = documentCatalogPort;
    }

    @Override
    public List<DocumentSummary> listDocuments() {
        return documentCatalogPort.listDocuments();
    }
}
