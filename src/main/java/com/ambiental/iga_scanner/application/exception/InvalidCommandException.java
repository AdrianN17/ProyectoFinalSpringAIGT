package com.ambiental.iga_scanner.application.exception;

/** Application-layer guardrail: a use-case command failed its own input validation. */
public class InvalidCommandException extends RuntimeException {

    public InvalidCommandException(String message) {
        super(message);
    }
}
