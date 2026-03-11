package com.anushibinj.resttosmtp.service;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Core email dispatching service.
 *
 * <p>The {@link #sendEmail(EmailProxyRequest)} method is annotated {@code @Async}, so Spring
 * dispatches it on the virtual-thread executor configured in
 * {@link com.anushibinj.resttosmtp.config.AsyncConfig}. The HTTP request thread is released
 * immediately after the controller hands off to this service, allowing the server to
 * handle new incoming requests while SMTP I/O happens in the background.
 *
 * <h2>Message assembly rules</h2>
 * <ol>
 *   <li>If only {@code text} is present → {@code text/plain} message.</li>
 *   <li>If {@code html} is present → {@code multipart/alternative} with both parts;
 *       the HTML body is preferred by modern clients, plain-text acts as fallback.</li>
 *   <li>If {@code icalEvent} is present → {@code invite.ics} is attached with
 *       {@code Content-Type: text/calendar; method=REQUEST}, which calendar clients
 *       (Google Calendar, Outlook, Apple Calendar) parse automatically.</li>
 * </ol>
 */
@Slf4j
@Service
public class EmailService {

    private final MailSenderFactory mailSenderFactory;
    private final MeterRegistry meterRegistry;
    
    private Counter emailDispatchSuccessCounter;
    private Counter emailDispatchFailureCounter;
    private Timer emailDispatchLatencyTimer;

    @Autowired
    public EmailService(MailSenderFactory mailSenderFactory, @Autowired(required = false) MeterRegistry meterRegistry) {
        this.mailSenderFactory = mailSenderFactory;
        this.meterRegistry = meterRegistry;
        initializeMetricsIfNeeded();
    }

    /**
     * Lazy initialization of metrics on first use to avoid MeterRegistry null issues in tests.
     */
    private void initializeMetricsIfNeeded() {
        if (emailDispatchSuccessCounter == null && meterRegistry != null) {
            this.emailDispatchSuccessCounter = meterRegistry.counter("email.dispatch.success.total", "service", "rest-to-smtp");
            this.emailDispatchFailureCounter = meterRegistry.counter("email.dispatch.failure.total", "service", "rest-to-smtp");
            this.emailDispatchLatencyTimer = meterRegistry.timer("email.dispatch.latency", "service", "rest-to-smtp");
        }
    }

    /**
     * Asynchronously builds and sends the email described by {@code request}.
     *
     * <p>Failures are logged at {@code ERROR} level and swallowed — this is intentional for
     * a fire-and-forget proxy. Callers receive HTTP 202 Accepted before this method completes,
     * so there is no mechanism to propagate exceptions back to the original HTTP client.
     * Operators should monitor logs or wire up a dead-letter mechanism if guaranteed delivery
     * is required.
     *
     * @param request the fully validated email proxy request
     */
    @Async
    public void sendEmail(EmailProxyRequest request) {
        log.info("Dispatching email to={} from={} host={}:{}",
                request.getTo(), request.getFrom(),
                request.getSmtpHost(), request.getSmtpPort());
        
        if (emailDispatchLatencyTimer != null) {
            emailDispatchLatencyTimer.record(() -> dispatchEmail(request));
        } else {
            dispatchEmail(request);
        }
    }

    /**
     * Internal method to handle email dispatch with error handling.
     */
    private void dispatchEmail(EmailProxyRequest request) {
        try {
            JavaMailSender sender = mailSenderFactory.createMailSender(request);
            MimeMessage message = sender.createMimeMessage();

            boolean isMultipart = StringUtils.hasText(request.getHtml())
                    || StringUtils.hasText(request.getIcalEvent());

            MimeMessageHelper helper = new MimeMessageHelper(message, isMultipart, StandardCharsets.UTF_8.name());

            helper.setFrom(request.getFrom());
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());

            if (StringUtils.hasText(request.getHtml())) {
                // multipart/alternative: plain-text (fallback) + HTML
                String textBody = StringUtils.hasText(request.getText()) ? request.getText() : "";
                helper.setText(textBody, request.getHtml());
            } else {
                // plain-text only — text is guaranteed non-blank by @AtLeastOneBodyPresent
                helper.setText(request.getText(), false);
            }

            if (StringUtils.hasText(request.getIcalEvent())) {
                byte[] icalBytes = request.getIcalEvent().getBytes(StandardCharsets.UTF_8);
                helper.addAttachment(
                        "invite.ics",
                        new ByteArrayResource(icalBytes),
                        "text/calendar; method=REQUEST"
                );
            }

            sender.send(message);
            log.info("Email successfully dispatched to={} subject=\"{}\"",
                    request.getTo(), request.getSubject());
            if (emailDispatchSuccessCounter != null) {
                emailDispatchSuccessCounter.increment();
            }

        } catch (MessagingException e) {
            log.error("Failed to send email to={} via host={}:{} — MessagingException: {}",
                    request.getTo(), request.getSmtpHost(), request.getSmtpPort(), e.getMessage(), e);
            if (emailDispatchFailureCounter != null) {
                emailDispatchFailureCounter.increment();
            }
        } catch (Exception e) {
            log.error("Unexpected error while sending email to={} via host={}:{} — {}",
                    request.getTo(), request.getSmtpHost(), request.getSmtpPort(), e.getMessage(), e);
            if (emailDispatchFailureCounter != null) {
                emailDispatchFailureCounter.increment();
            }
        }
    }
}
