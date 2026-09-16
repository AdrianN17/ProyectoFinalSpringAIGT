package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Enforces a hard minimum interval between successive outbound calls to the Azure AI
 * Foundry embeddings endpoint. Azure's lower pricing tiers (e.g. S0) impose a strict
 * per-minute call-rate limit regardless of how many texts are batched into a single
 * request, so every actual network call — whether it is the first attempt for a batch or
 * a retry after a 429 — must wait its turn here before being sent.
 */
@Component
class EmbeddingRequestPacer {

    private static final Duration MIN_INTERVAL = Duration.ofSeconds(30);

    private final Object lock = new Object();
    private Instant nextAllowedCallAt = Instant.EPOCH;

    /**
     * Blocks the calling thread, if necessary, until at least {@link #MIN_INTERVAL} has
     * elapsed since the previous call was allowed through.
     */
    void awaitTurn() {
        Duration wait;
        synchronized (lock) {
            Instant now = Instant.now();
            wait = Duration.between(now, nextAllowedCallAt);
            Instant effectiveStart = wait.isPositive() ? nextAllowedCallAt : now;
            nextAllowedCallAt = effectiveStart.plus(MIN_INTERVAL);
        }
        if (wait.isPositive()) {
            sleep(wait);
        }
    }

    private void sleep(Duration wait) {
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while pacing Azure embeddings calls", ex);
        }
    }
}
