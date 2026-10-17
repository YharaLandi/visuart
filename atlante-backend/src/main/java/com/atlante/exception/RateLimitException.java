package com.atlante.exception;

/** Il servizio AI esterno ha risposto 429. */
public class RateLimitException extends RuntimeException {

    private final String retryAfter;

    public RateLimitException(String retryAfter) {
        super("Limite di richieste AI raggiunto");
        this.retryAfter = retryAfter;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}
