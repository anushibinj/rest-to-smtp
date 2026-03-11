package com.anushibinj.resttosmtp.exception;

import com.anushibinj.resttosmtp.dto.ValidationErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for {@link GlobalExceptionHandler} — invoked without a Spring context
 * so we can trigger the {@code ConstraintViolationException} handler, which is only
 * reachable at runtime via service-level {@code @Validated}, not via MockMvc {@code @RequestBody}.
 */
@DisplayName("GlobalExceptionHandler (unit)")
class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unchecked")
    private ConstraintViolationException buildConstraintViolationException(String propertyPath, String message) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(propertyPath);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return new ConstraintViolationException("Validation failed", Set.of(violation));
    }

    // ── ConstraintViolationException handler ─────────────────────────────────

    @Test
    @DisplayName("handleConstraintViolation returns 400")
    void constraintViolation_returns400() {
        ConstraintViolationException ex = buildConstraintViolationException("smtpPort", "must be between 1 and 65535");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("handleConstraintViolation response contains status 400")
    void constraintViolation_bodyHasStatus400() {
        ConstraintViolationException ex = buildConstraintViolationException("smtpPort", "must be between 1 and 65535");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        assertThat(response.getBody()).containsEntry("status", 400);
    }

    @Test
    @DisplayName("handleConstraintViolation response contains 'errors' list")
    void constraintViolation_bodyHasErrors() {
        ConstraintViolationException ex = buildConstraintViolationException("smtpPort", "must be between 1 and 65535");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        assertThat(response.getBody()).containsKey("errors");
        Object errors = response.getBody().get("errors");
        assertThat(errors).isInstanceOf(List.class);
        assertThat((List<?>) errors).isNotEmpty();
    }

    @Test
    @DisplayName("handleConstraintViolation response 'errors' entry has 'field' matching property path")
    void constraintViolation_errorHasField() {
        ConstraintViolationException ex = buildConstraintViolationException("myField", "some message");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        List<Map<String, String>> errors = (List<Map<String, String>>) response.getBody().get("errors");
        assertThat(errors.get(0)).containsEntry("field", "myField");
    }

    @Test
    @DisplayName("handleConstraintViolation response 'errors' entry has 'message' matching violation message")
    void constraintViolation_errorHasMessage() {
        ConstraintViolationException ex = buildConstraintViolationException("myField", "some error message");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        List<Map<String, String>> errors = (List<Map<String, String>>) response.getBody().get("errors");
        assertThat(errors.get(0)).containsEntry("message", "some error message");
    }

    @Test
    @DisplayName("handleConstraintViolation response contains 'timestamp' field")
    void constraintViolation_bodyHasTimestamp() {
        ConstraintViolationException ex = buildConstraintViolationException("f", "m");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleConstraintViolation response contains 'error' = 'Bad Request'")
    void constraintViolation_bodyHasErrorField() {
        ConstraintViolationException ex = buildConstraintViolationException("f", "m");
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
    }

    // ── ValidationErrorResponse inner records ─────────────────────────────────
    // These are schema-documentation stubs; exercise their constructors/accessors for coverage.

    @Test
    @DisplayName("ValidationErrorResponse.FieldError record constructs and accessors work")
    void fieldErrorRecord_constructsCorrectly() {
        ValidationErrorResponse.FieldError fe =
                new ValidationErrorResponse.FieldError("myField", "must not be blank");
        assertThat(fe.field()).isEqualTo("myField");
        assertThat(fe.message()).isEqualTo("must not be blank");
    }

    @Test
    @DisplayName("ValidationErrorResponse record constructs and accessors work")
    void validationErrorResponse_constructsCorrectly() {
        ValidationErrorResponse.FieldError fe =
                new ValidationErrorResponse.FieldError("f", "m");
        ValidationErrorResponse resp =
                new ValidationErrorResponse(
                        "2024-01-01T00:00:00Z", 400, "Bad Request", "Validation failed", List.of(fe));
        assertThat(resp.status()).isEqualTo(400);
        assertThat(resp.error()).isEqualTo("Bad Request");
        assertThat(resp.message()).isEqualTo("Validation failed");
        assertThat(resp.errors()).hasSize(1);
    }
}
