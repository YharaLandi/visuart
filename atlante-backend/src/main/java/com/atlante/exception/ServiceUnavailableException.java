package com.atlante.exception;

/** Il servizio AI esterno non e' raggiungibile o ha risposto 5xx. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
