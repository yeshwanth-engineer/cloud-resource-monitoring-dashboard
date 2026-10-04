package com.yeshwanth.cloudmonitor.web;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Uniform error body for every non-2xx API response, so the dashboard can display
 * {@code message} instead of a generic "request failed".
 */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Instant.now());
    }
}
