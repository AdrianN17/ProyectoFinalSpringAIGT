package com.ambiental.iga_scanner.application.port.in;

import com.ambiental.iga_scanner.domain.IngestedDocument;

@FunctionalInterface
public interface UploadPdfUseCase {
    IngestedDocument upload(UploadPdfCommand command);
}
