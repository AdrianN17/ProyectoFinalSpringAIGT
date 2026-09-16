package com.ambiental.iga_scanner.application.port.in;

import com.ambiental.iga_scanner.domain.DocumentSummary;
import java.util.List;

@FunctionalInterface
public interface ListDocumentsUseCase {
    List<DocumentSummary> listDocuments();
}
