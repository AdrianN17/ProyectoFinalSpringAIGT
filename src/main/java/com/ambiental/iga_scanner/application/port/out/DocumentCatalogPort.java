package com.ambiental.iga_scanner.application.port.out;

import com.ambiental.iga_scanner.domain.IngestedDocument;
import java.util.List;

public interface DocumentCatalogPort {
    List<String> listDocumentNames();

    void recordUpload(IngestedDocument document);
}
