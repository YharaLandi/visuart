package com.atlante.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DuplicateLikeException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateLike(DuplicateLikeException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        return error(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Corpo della richiesta non leggibile");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Richiesta non valida");
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    // ---- servizio AI esterno

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitException e) {
        HttpHeaders headers = new HttpHeaders();
        if (e.getRetryAfter() != null) {
            headers.add(HttpHeaders.RETRY_AFTER, e.getRetryAfter());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers)
                .body(Map.of("error", "Troppe richieste all'AI in questo momento, riprova tra poco"));
    }

    @ExceptionHandler(ExternalRequestInvalidException.class)
    public ResponseEntity<Map<String, String>> handleExternalInvalid(ExternalRequestInvalidException e) {
        return error(HttpStatus.BAD_GATEWAY, "Errore di configurazione del servizio AI (chiave o modello)");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleServiceUnavailable(ServiceUnavailableException e) {
        return error(HttpStatus.BAD_GATEWAY, "Il servizio AI non è raggiungibile in questo momento, riprova");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
