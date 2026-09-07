package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

interface PdfUploadControllerMessageError {
    String EMPTY_FILE = "El archivo esta vacio";
    String UNSUPPORTED_MEDIA_TYPE = "Solo se admiten archivos PDF";
    String FILE_READ_ERROR = "No se pudo leer el archivo subido";
}
