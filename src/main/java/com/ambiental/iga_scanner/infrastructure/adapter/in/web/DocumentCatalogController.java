package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

import com.ambiental.iga_scanner.application.port.in.ListDocumentsUseCase;
import com.ambiental.iga_scanner.infrastructure.adapter.in.web.dto.DocumentSummaryDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DocumentCatalogController {

    private final ListDocumentsUseCase listDocumentsUseCase;

    DocumentCatalogController(ListDocumentsUseCase listDocumentsUseCase) {
        this.listDocumentsUseCase = listDocumentsUseCase;
    }

    @GetMapping("/api/documents")
    List<DocumentSummaryDto> listDocuments() {
        return listDocumentsUseCase.listDocuments().stream().map(DocumentSummaryDto::from).toList();
    }
}
