package com.ambiental.iga_scanner.application.port.in;

interface UploadPdfCommandMessageError {
    String BLANK_FILE_NAME = "El nombre del archivo no puede estar vacio";
    String EMPTY_FILE_CONTENT = "El archivo no puede estar vacio";
}
