package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

interface PdfIngestionAdapterMessageError {
    String ORIGINAL_STAGING_ERROR = "No se pudo escribir el PDF original en /process";
    String MARKDOWN_STAGING_ERROR = "No se pudo escribir el markdown convertido en /process";
}
