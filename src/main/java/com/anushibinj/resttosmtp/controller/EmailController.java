package com.anushibinj.resttosmtp.controller;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import com.anushibinj.resttosmtp.dto.ValidationErrorResponse;
import com.anushibinj.resttosmtp.service.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the single email-proxy endpoint.
 *
 * <p>The endpoint is intentionally fire-and-forget: validation succeeds → SMTP dispatch is
 * queued on a virtual thread → HTTP 202 Accepted is returned immediately. This design
 * maximises throughput because the web thread is never blocked on network I/O.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(
        name = "Email Proxy",
        description = "Stateless REST-to-SMTP proxy. Supply full SMTP credentials with each request."
)
public class EmailController {

    private final EmailService emailService;
    private final MeterRegistry meterRegistry;
    
    private Counter emailSendRequestsCounter;
    private Timer requestProcessingTimer;

    @Autowired
    public EmailController(EmailService emailService, @Autowired(required = false) MeterRegistry meterRegistry) {
        this.emailService = emailService;
        this.meterRegistry = meterRegistry;
        initializeMetricsIfNeeded();
    }

    /**
     * Lazy initialization of metrics on first use to avoid MeterRegistry null issues in tests.
     */
    private void initializeMetricsIfNeeded() {
        if (emailSendRequestsCounter == null && meterRegistry != null) {
            this.emailSendRequestsCounter = meterRegistry.counter("email.send.requests.total", "service", "rest-to-smtp");
            this.requestProcessingTimer = meterRegistry.timer("email.request.processing.time", "service", "rest-to-smtp");
        }
    }

    /**
     * Accepts an email send request, validates it, dispatches it asynchronously, and
     * returns HTTP 202 Accepted before SMTP delivery completes.
     *
     * @param request validated email proxy payload
     * @return 202 Accepted on successful queuing, 400 Bad Request on validation failure
     */
    @Operation(
            summary = "Send an email via a dynamically specified SMTP server",
            description = """
                    Accepts a fully self-contained email request — SMTP credentials and message payload
                    in one JSON body. The email is dispatched asynchronously on a virtual thread.
                    The caller receives HTTP 202 immediately; delivery happens in the background.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Email accepted for delivery. SMTP dispatch is in progress asynchronously."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — one or more required fields are missing or malformed.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            )
    })
    @PostMapping(
            value = "/send",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody EmailProxyRequest request) {
        log.debug("Received send request for to={}", request.getTo());
        if (emailSendRequestsCounter != null) {
            emailSendRequestsCounter.increment();
        }
        if (requestProcessingTimer != null) {
            requestProcessingTimer.record(() -> emailService.sendEmail(request));
        } else {
            emailService.sendEmail(request);
        }
        return ResponseEntity.accepted().build();
    }
}
