package com.atlante.exception;

/** Il servizio AI esterno ha rifiutato la richiesta (4xx): di solito chiave o modello errati. */
public class ExternalRequestInvalidException extends RuntimeException {
    public ExternalRequestInvalidException(String message) {
        super(message);
    }
}
