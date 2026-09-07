package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

/** AI-adapter guardrail: the call to the Azure AI Foundry chat model endpoint failed. */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
