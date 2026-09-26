package com.polypilot.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * Single point of translation from exceptions to HTTP responses. Every body is
 * {@link ErrorResponse} with one {@code message} field, matching what the dashboard's
 * {@code readErrorMessage} (({@code body.detail ?? body.message})) already expects — see
 * {@code spring.web.error.include-message} in {@code application.yml}.
 *
 * <p>Handlers are ordered most-specific first. Exceptions already caught and converted locally
 * (e.g. {@code SiweService.parse} catching {@link IllegalArgumentException} for a non-revealing
 * 401) never reach this class.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        if (status.is5xxServerError()) {
            log.error("Unhandled server error: [{}]", ex.getReason(), ex);
        } else {
            log.debug("Request rejected: [{}] [{}]", status, ex.getReason());
        }
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("Request body validation failed: [{}]", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: [{}]", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Malformed request body"));
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse> handleEncryption(EncryptionException ex) {
        log.error("Encryption operation failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Encryption operation failed"));
    }

    /** Downstream call to auth-service failed (network error or non-2xx response). */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClient(RestClientException ex) {
        log.error("Upstream service call failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Upstream service error"));
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleIllegalState(RuntimeException ex) {
        log.error("Internal error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal error"));
    }
}
