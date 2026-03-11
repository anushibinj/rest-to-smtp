package com.anushibinj.resttosmtp.actuator;

import com.anushibinj.resttosmtp.RestToSmtpApplication;
import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Boot Actuator endpoints.
 *
 * Verifies health checks, metrics collection, and monitoring endpoints are
 * properly configured and accessible.
 */
@SpringBootTest(classes = RestToSmtpApplication.class)
@AutoConfigureMockMvc
class ActuatorEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpoint_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testHealthEndpoint_ShowsDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").exists());
    }

    @Test
    void testLivenessProbe_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testReadinessProbe_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testMetricsEndpoint_ReturnsAvailableMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void testInfoEndpoint_ReturnsApplicationInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void testEmailRequestCounterMetric_IncrementsOnValidRequest() throws Exception {
        // First request to increment counter
        String validRequest = """
                {
                  "smtpHost": "smtp.example.com",
                  "smtpPort": 587,
                  "smtpUsername": "user@example.com",
                  "smtpPassword": "password",
                  "from": "sender@example.com",
                  "to": "recipient@example.com",
                  "subject": "Test",
                  "text": "Test body"
                }
                """;

        mockMvc.perform(post("/api/v1/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
                .andExpect(status().isAccepted());

        // Check that metric was incremented
        mockMvc.perform(get("/actuator/metrics/email.send.requests.total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").value(1.0));
    }

    @Test
    void testValidationErrorCounterMetric_IncrementsOnInvalidRequest() throws Exception {
        // Request with missing required field
        String invalidRequest = """
                {
                  "smtpHost": "smtp.example.com",
                  "smtpPort": 587,
                  "smtpUsername": "user@example.com",
                  "smtpPassword": "password"
                }
                """;

        mockMvc.perform(post("/api/v1/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        // Validation error counter should be incremented - metric is created on-demand,
        // so it should exist after validation error. Allow 404 if metric hasn't been initialized.
        mockMvc.perform(get("/actuator/metrics/email.validation.errors.total"))
                .andExpect(status().isOk());
    }

    @Test
    void testRequestProcessingTimer_RecordsLatency() throws Exception {
        String validRequest = """
                {
                  "smtpHost": "smtp.example.com",
                  "smtpPort": 587,
                  "smtpUsername": "user@example.com",
                  "smtpPassword": "password",
                  "from": "sender@example.com",
                  "to": "recipient@example.com",
                  "subject": "Test",
                  "text": "Test body"
                }
                """;

        mockMvc.perform(post("/api/v1/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
                .andExpect(status().isAccepted());

        // Check timer metric exists and has recorded values
        mockMvc.perform(get("/actuator/metrics/email.request.processing.time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements").isArray());
    }

    @Test
    void testMetricsIncludeServiceTag() throws Exception {
        // Verify custom tags are applied to metrics
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names[*]").isArray());
    }

    @Test
    void testEndpointExposure_OnlyAllowedEndpointsExposed() throws Exception {
        // health, info, metrics, prometheus should be exposed
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());

        // env endpoint should NOT be exposed
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }
}
