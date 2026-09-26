package com.polypilot.common.exception;

import lombok.Value;

/**
 * Standard error body returned by {@link GlobalExceptionHandler}. Matches the shape the
 * dashboard already expects from Boot's default error controller (see
 * {@code spring.web.error.include-message} in {@code application.yml}): a single
 * {@code message} field, read by {@code readErrorMessage} as {@code body.detail ?? body.message}.
 */
@Value
public class ErrorResponse {
    String message;
}
