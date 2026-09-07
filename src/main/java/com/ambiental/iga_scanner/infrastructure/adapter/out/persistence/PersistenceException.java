package com.ambiental.iga_scanner.infrastructure.adapter.out.persistence;

/** Persistence-adapter guardrail: a database operation failed. */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
