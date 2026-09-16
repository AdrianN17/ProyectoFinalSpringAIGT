package com.ambiental.iga_scanner.infrastructure.adapter.out.pdf;

/** PDF-adapter guardrail: staging the original file, the Markdown, or chunking/indexing failed. */
public class PdfProcessingException extends RuntimeException {

    public PdfProcessingException(String message) {
        super(message);
    }

    public PdfProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
