package com.ambiental.iga_scanner.infrastructure.adapter.in.web;

/** Web-adapter guardrail: the uploaded multipart file could not be read from the request. */
class FileReadException extends RuntimeException {

    FileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
