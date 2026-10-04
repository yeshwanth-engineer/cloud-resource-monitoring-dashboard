package com.yeshwanth.cloudmonitor.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.stream.Collectors;

/**
 * Turns unhandled exceptions into the same JSON shape the rest of the API uses, and hides AWS
 * plumbing details from the client while keeping them in the log.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Errors thrown on purpose by controllers (bad input, bad credentials). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }

    /** Bean validation failures on request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message));
    }

    /**
     * Anything the AWS SDK rejects: missing credentials, a region without permission, a throttled
     * request. It is a server-side dependency problem, so it maps to 502 rather than 500.
     */
    @ExceptionHandler(SdkException.class)
    public ResponseEntity<ErrorResponse> handleAws(SdkException exception) {
        log.warn("AWS request failed: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse.of(HttpStatus.BAD_GATEWAY,
                "The AWS API request failed. Check credentials, region and IAM permissions"
                        + " (ec2:DescribeInstances, cloudwatch:GetMetricData), or run with DEMO_MODE=true."));
    }

    /** Last resort: log the stack trace, return a generic message. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled exception while serving request", exception);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error"));
    }

    private String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}
