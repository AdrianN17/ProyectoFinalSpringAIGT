package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

/** AI-adapter guardrail: the similarity search against the Qdrant vector store failed. */
public class RetrievalFailedException extends RuntimeException {

    public RetrievalFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
