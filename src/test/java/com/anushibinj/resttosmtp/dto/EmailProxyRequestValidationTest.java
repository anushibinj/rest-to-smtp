package com.anushibinj.resttosmtp.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmailProxyRequest} Bean Validation constraints.
 *
 * <p>Uses a standalone {@link Validator} instance — no Spring context required.
 */
@DisplayName("EmailProxyRequest validation")
class EmailProxyRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private EmailProxyRequest validRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("Subject")
                .text("Body")
                .build();
    }

    private Set<ConstraintViolation<EmailProxyRequest>> validate(EmailProxyRequest req) {
        return validator.validate(req);
    }

    // ── Valid request produces no violations ──────────────────────────────────

    @Test
    @DisplayName("fully valid request produces zero violations")
    void valid_noViolations() {
        assertThat(validate(validRequest())).isEmpty();
    }

    @Test
    @DisplayName("valid request with optional fields also passes")
    void validWithOptionals_noViolations() {
        EmailProxyRequest req = validRequest();
        req.setHtml("<h1>Hi</h1>");
        req.setIcalEvent("BEGIN:VCALENDAR\nEND:VCALENDAR");
        assertThat(validate(req)).isEmpty();
    }

    // ── smtpHost ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null smtpHost violates @NotBlank")
    void nullSmtpHost_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpHost"));
    }

    @Test
    @DisplayName("blank smtpHost violates @NotBlank")
    void blankSmtpHost_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost("  ");
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpHost"));
    }

    // ── smtpPort ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null smtpPort violates @NotNull")
    void nullSmtpPort_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpPort"));
    }

    @Test
    @DisplayName("smtpPort = 0 violates @Min(1)")
    void smtpPortZero_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(0);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpPort"));
    }

    @Test
    @DisplayName("smtpPort = -1 violates @Min(1)")
    void smtpPortNegative_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(-1);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpPort"));
    }

    @Test
    @DisplayName("smtpPort = 65536 violates @Max(65535)")
    void smtpPortAboveMax_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(65536);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpPort"));
    }

    @Test
    @DisplayName("smtpPort = 1 passes @Min(1)")
    void smtpPortMin_valid() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(1);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("smtpPort = 65535 passes @Max(65535)")
    void smtpPortMax_valid() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(65535);
        assertThat(validate(req)).isEmpty();
    }

    // ── smtpUsername ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("null smtpUsername violates @NotBlank")
    void nullSmtpUsername_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpUsername(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpUsername"));
    }

    @Test
    @DisplayName("blank smtpUsername violates @NotBlank")
    void blankSmtpUsername_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpUsername("");
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpUsername"));
    }

    // ── smtpPassword ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("null smtpPassword violates @NotBlank")
    void nullSmtpPassword_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPassword(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpPassword"));
    }

    @Test
    @DisplayName("blank smtpPassword violates @NotBlank")
    void blankSmtpPassword_violation() {
        EmailProxyRequest req = validRequest();
        req.setSmtpPassword("   ");
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("smtpPassword"));
    }

    // ── to ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null 'to' violates @NotBlank")
    void nullTo_violation() {
        EmailProxyRequest req = validRequest();
        req.setTo(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("to"));
    }

    @Test
    @DisplayName("invalid 'to' email violates @Email")
    void invalidTo_violation() {
        EmailProxyRequest req = validRequest();
        req.setTo("not-an-email");
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("to"));
    }

    @Test
    @DisplayName("valid 'to' email with subdomain passes")
    void validToWithSubdomain_noViolation() {
        EmailProxyRequest req = validRequest();
        req.setTo("user@mail.example.co.uk");
        assertThat(validate(req)).isEmpty();
    }

    // ── from ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null 'from' violates @NotBlank")
    void nullFrom_violation() {
        EmailProxyRequest req = validRequest();
        req.setFrom(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("from"));
    }

    @Test
    @DisplayName("invalid 'from' email violates @Email")
    void invalidFrom_violation() {
        EmailProxyRequest req = validRequest();
        req.setFrom("bad-email");
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("from"));
    }

    // ── subject ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null subject violates @NotBlank")
    void nullSubject_violation() {
        EmailProxyRequest req = validRequest();
        req.setSubject(null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("subject"));
    }

    @Test
    @DisplayName("blank subject violates @NotBlank")
    void blankSubject_violation() {
        EmailProxyRequest req = validRequest();
        req.setSubject("  ");
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("subject"));
    }

    // ── text / html (at-least-one body) ──────────────────────────────────────

    @Test
    @DisplayName("only text provided — valid")
    void onlyText_valid() {
        EmailProxyRequest req = validRequest(); // validRequest() sets text="Body", html=null
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("only html provided — valid")
    void onlyHtml_valid() {
        EmailProxyRequest req = validRequest();
        req.setText(null);
        req.setHtml("<h1>Hello</h1>");
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("both text and html provided — valid")
    void bothTextAndHtml_valid() {
        EmailProxyRequest req = validRequest();
        req.setHtml("<h1>Hello</h1>");
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("neither text nor html (both null) violates @AtLeastOneBodyPresent")
    void bothNull_violation() {
        EmailProxyRequest req = validRequest();
        req.setText(null);
        req.setHtml(null);
        assertThat(validate(req)).isNotEmpty();
    }

    @Test
    @DisplayName("neither text nor html (both blank) violates @AtLeastOneBodyPresent")
    void bothBlank_violation() {
        EmailProxyRequest req = validRequest();
        req.setText("  ");
        req.setHtml("");
        assertThat(validate(req)).isNotEmpty();
    }

    // ── optional fields ───────────────────────────────────────────────────────

    @Test
    @DisplayName("null html is allowed (optional field)")
    void nullHtml_allowed() {
        EmailProxyRequest req = validRequest();
        req.setHtml(null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    @DisplayName("null icalEvent is allowed (optional field)")
    void nullIcalEvent_allowed() {
        EmailProxyRequest req = validRequest();
        req.setIcalEvent(null);
        assertThat(validate(req)).isEmpty();
    }

    // ── Lombok-generated methods ───────────────────────────────────────────────

    @Test
    @DisplayName("equals works correctly for identical requests")
    void equals_identicalRequests() {
        EmailProxyRequest r1 = validRequest();
        EmailProxyRequest r2 = validRequest();
        assertThat(r1).isEqualTo(r2);
    }

    @Test
    @DisplayName("equals returns false for different requests")
    void equals_differentRequests() {
        EmailProxyRequest r1 = validRequest();
        EmailProxyRequest r2 = validRequest();
        r2.setTo("different@example.com");
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("hashCode is consistent for equal objects")
    void hashCode_consistentForEqualObjects() {
        EmailProxyRequest r1 = validRequest();
        EmailProxyRequest r2 = validRequest();
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("toString contains smtpHost")
    void toString_containsSmtpHost() {
        EmailProxyRequest req = validRequest();
        assertThat(req.toString()).contains("smtp.example.com");
    }
}
