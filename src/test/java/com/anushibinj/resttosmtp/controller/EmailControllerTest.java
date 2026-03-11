package com.anushibinj.resttosmtp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import com.anushibinj.resttosmtp.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link EmailController} using {@code @WebMvcTest}.
 *
 * <p>Spring wires up only the web layer (MockMvc, Jackson, validation, exception handlers).
 * {@link EmailService} is replaced by a Mockito bean so no real SMTP work happens.
 */
@WebMvcTest(EmailController.class)
@DisplayName("EmailController")
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EmailProxyRequest validRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("Test Subject")
                .text("Hello world")
                .build();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ── Happy-path tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("valid request returns HTTP 202 Accepted")
    void validRequest_returns202() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("valid request delegates to EmailService once")
    void validRequest_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isAccepted());

        verify(emailService, times(1)).sendEmail(any(EmailProxyRequest.class));
    }

    @Test
    @DisplayName("request with optional html field returns 202")
    void requestWithHtml_returns202() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setHtml("<h1>Hello</h1>");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("request with optional icalEvent field returns 202")
    void requestWithIcal_returns202() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setIcalEvent("BEGIN:VCALENDAR\nMETHOD:REQUEST\nEND:VCALENDAR");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("request with all optional fields returns 202")
    void requestWithAllFields_returns202() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setHtml("<h1>Hello</h1>");
        req.setIcalEvent("BEGIN:VCALENDAR\nMETHOD:REQUEST\nEND:VCALENDAR");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isAccepted());
    }

    // ── Validation failure tests ───────────────────────────────────────────────

    @Test
    @DisplayName("missing smtpHost returns 400")
    void missingSmtpHost_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing smtpHost: service is never called")
    void missingSmtpHost_serviceNeverCalled() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @DisplayName("missing smtpPort returns 400")
    void missingSmtpPort_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("smtpPort = 0 (below min) returns 400")
    void smtpPortBelowMin_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(0);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("smtpPort = 65536 (above max) returns 400")
    void smtpPortAboveMax_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(65536);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("smtpPort = 1 (min boundary) returns 202")
    void smtpPortAtMin_returns202() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(1);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("smtpPort = 65535 (max boundary) returns 202")
    void smtpPortAtMax_returns202() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpPort(65535);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("missing smtpUsername returns 400")
    void missingSmtpUsername_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpUsername(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("blank smtpUsername returns 400")
    void blankSmtpUsername_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpUsername("   ");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing smtpPassword returns 400")
    void missingSmtpPassword_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpPassword(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid 'to' email returns 400")
    void invalidToEmail_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setTo("not-an-email");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid 'from' email returns 400")
    void invalidFromEmail_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setFrom("not-an-email");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing subject returns 400")
    void missingSubject_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSubject(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("blank subject returns 400")
    void blankSubject_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSubject("");

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing text returns 400")
    void missingText_returns400() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setText(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Validation error response structure tests ─────────────────────────────

    @Test
    @DisplayName("400 response contains 'status' field with value 400")
    void validationError_containsStatusField() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("400 response contains 'errors' array")
    void validationError_containsErrorsArray() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("400 response contains 'timestamp' field")
    void validationError_containsTimestamp() throws Exception {
        EmailProxyRequest req = validRequest();
        req.setSmtpHost(null);

        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("empty body returns 400")
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
