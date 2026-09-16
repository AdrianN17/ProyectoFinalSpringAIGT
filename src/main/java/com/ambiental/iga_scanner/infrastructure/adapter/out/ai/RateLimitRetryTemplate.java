package com.ambiental.iga_scanner.infrastructure.adapter.out.ai;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Retries a call to an upstream AI provider when it responds with HTTP 429 (Too Many
 * Requests), honoring the provider's {@code Retry-After} header when present and falling
 * back to exponential backoff otherwise. Azure AI Foundry's lower pricing tiers (e.g. S0)
 * enforce tight per-minute call-rate limits, so transient 429s are expected under load and
 * should not immediately surface as failures to the caller.
 */
final class RateLimitRetryTemplate {

    private static final Logger log = LoggerFactory.getLogger(RateLimitRetryTemplate.class);

    private static final int MAX_ATTEMPTS = 4;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    private RateLimitRetryTemplate() {
    }

    static <T> T execute(String operationName, Supplier<T> call) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return call.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.error("{} failed after {} attempts due to repeated rate limiting", operationName, attempt, e);
                    throw e;
                }
                Duration wait = retryAfter(e).orElse(backoff(attempt));
                log.warn("{} was rate limited (attempt {}/{}); retrying in {}", operationName, attempt, MAX_ATTEMPTS,
                        wait);
                sleep(wait);
            }
        }
    }

    private static java.util.Optional<Duration> retryAfter(RestClientResponseException e) {
        List<String> values = e.getResponseHeaders() == null
                ? List.of()
                : e.getResponseHeaders().getOrEmpty(HttpHeaders.RETRY_AFTER);
        if (values.isEmpty()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Duration.ofSeconds(Long.parseLong(values.get(0).trim())));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    private static Duration backoff(int attempt) {
        Duration candidate = INITIAL_BACKOFF.multipliedBy(1L << (attempt - 1));
        return candidate.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : candidate;
    }

    private static void sleep(Duration wait) {
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry after rate limiting", ex);
        }
    }
}
