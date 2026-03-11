package com.anushibinj.resttosmtp.exception;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import com.anushibinj.resttosmtp.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anushibinj.resttosmtp.controller.EmailController;

/**
 * Focused tests for {@link GlobalExceptionHandler} verifying the shape and content
 * of error responses produced for Bean Validation failures.
 *
 * <p>Uses {@code @WebMvcTest} targeting the controller so the full MVC stack
 * (including exception handlers) is active.
 */
@WebMvcTest(EmailController.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    // ── Error envelope structure ───────────────────────────────────────────────

    @Test
    @DisplayName("error response has 'timestamp' key")
    void errorResponse_hasTimestamp() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("error response has 'status' = 400")
    void errorResponse_hasStatus400() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("error response has 'error' = 'Bad Request'")
    void errorResponse_hasBadRequestError() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("error response has 'message' field")
    void errorResponse_hasMessage() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("error response has non-empty 'errors' array")
    void errorResponse_hasNonEmptyErrors() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("each error entry has 'field' key")
    void errorEntry_hasFieldKey() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").exists());
    }

    @Test
    @DisplayName("each error entry has 'message' key")
    void errorEntry_hasMessageKey() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    // ── Multiple violations ───────────────────────────────────────────────────

    @Test
    @DisplayName("empty body produces multiple field errors (one per missing required field)")
    void emptyBody_producesMultipleErrors() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(1))));
    }

    // ── Specific field validation messages ───────────────────────────────────

    @Test
    @DisplayName("invalid port reports smtpPort field in errors")
    void invalidPort_reportsSmtpPortField() throws Exception {
        EmailProxyRequest req = EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(99999)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("Sub")
                .text("Body")
                .build();

        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'smtpPort')]").exists());
    }

    @Test
    @DisplayName("invalid 'to' email reports 'to' field in errors")
    void invalidTo_reportsToField() throws Exception {
        EmailProxyRequest req = EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("not-an-email")
                .from("from@example.com")
                .subject("Sub")
                .text("Body")
                .build();

        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'to')]").exists());
    }
}
