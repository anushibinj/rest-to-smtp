package com.anushibinj.resttosmtp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * OpenAPI response schema for HTTP 400 validation errors.
 *
 * <p>Declared as a top-level type so springdoc can register it as a named component
 * ({@code #/components/schemas/ValidationErrorResponse}) and the {@code $ref} in
 * {@link com.anushibinj.resttosmtp.controller.EmailController} resolves correctly.
 */
@Schema(name = "ValidationErrorResponse", description = "Structured validation error response")
public record ValidationErrorResponse(
        @Schema(description = "ISO-8601 timestamp of the error", example = "2024-01-15T10:30:00Z")
        String timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP status reason", example = "Bad Request")
        String error,
        @Schema(description = "Error summary message", example = "Validation failed")
        String message,
        @Schema(description = "Per-field validation errors")
        List<FieldError> errors
) {
    @Schema(name = "FieldError", description = "Single field validation error")
    public record FieldError(
            @Schema(description = "Name of the invalid field", example = "smtpHost")
            String field,
            @Schema(description = "Validation failure reason", example = "must not be blank")
            String message
    ) {}
}
