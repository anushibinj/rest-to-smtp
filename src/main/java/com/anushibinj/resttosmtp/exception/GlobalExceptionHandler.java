package com.anushibinj.resttosmtp.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates Bean Validation failures into a structured JSON 400 response.
 *
 * <p>Without this handler Spring Boot would return its default error page, which leaks
 * internal stack trace information. Here we emit a minimal, consistent error envelope
 * that clients can parse reliably.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;
    private Counter validationErrorCounter;

    @Autowired(required = false)
    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /** No-arg constructor for unit tests that don't need metrics. */
    public GlobalExceptionHandler() {
        this(null);
    }

    private Counter getValidationErrorCounter() {
        if (validationErrorCounter == null && meterRegistry != null) {
            validationErrorCounter = meterRegistry.counter("email.validation.errors.total", "service", "rest-to-smtp");
        }
        return validationErrorCounter;
    }

    /**
     * Handles validation errors raised by {@code @Valid} on {@code @RequestBody} parameters.
     * Each field violation is collected into a {@code fields} map for easy client consumption.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("field", fe.getField());
                    entry.put("message", fe.getDefaultMessage());
                    return entry;
                })
                .toList();

        log.warn("Request validation failed — {} field error(s): {}", fieldErrors.size(), fieldErrors);

        Counter counter = getValidationErrorCounter();
        if (counter != null) {
            counter.increment(fieldErrors.size());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody("Validation failed", fieldErrors));
    }

    /**
     * Handles constraint violations raised by {@code @Validated} on service/controller params.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> violations = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("field", cv.getPropertyPath().toString());
                    entry.put("message", cv.getMessage());
                    return entry;
                })
                .toList();

        log.warn("Constraint violation — {} violation(s): {}", violations.size(), violations);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody("Constraint violation", violations));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildErrorBody(String message, Object errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", message);
        body.put("errors", errors);
        return body;
    }
}
