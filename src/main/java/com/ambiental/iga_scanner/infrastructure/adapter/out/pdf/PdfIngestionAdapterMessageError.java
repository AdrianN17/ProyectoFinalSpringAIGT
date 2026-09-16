package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

interface PdfIngestionAdapterMessageError {
    String ORIGINAL_STAGING_ERROR = "No se pudo escribir el PDF original en /process";
    String BASE_MARKDOWN_STAGING_ERROR = "No se pudo escribir el markdown base en /process";
    String CLASSIFICATION_STAGING_ERROR = "No se pudo escribir el markdown de clasificaci\u00f3n en /process";
    String SECTION_STAGING_ERROR = "No se pudo escribir el markdown de una secci\u00f3n en /process/extracted";
    String EXTRACTED_DIR_ERROR = "No se pudo crear la carpeta /process/extracted";
    String NO_SECTIONS_FOUND = "El markdown reorganizado por la IA no contiene secciones v\u00e1lidas";
    String FIDELITY_CHECK_FAILED = "El markdown reorganizado por la IA parece haber resumido u "
            + "omitido contenido del documento original; se aborta la carga para no indexar datos incompletos";
}
