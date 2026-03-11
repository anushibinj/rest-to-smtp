package com.anushibinj.resttosmtp.service;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailService}.
 *
 * <p>All SMTP I/O is mocked via {@link JavaMailSender} so tests run without a real server.
 * The {@code @Async} annotation is NOT triggered in plain unit tests (no Spring context),
 * so {@link EmailService#sendEmail} executes synchronously here — which is exactly what we
 * want for deterministic assertions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock
    private MailSenderFactory mailSenderFactory;

    @InjectMocks
    private EmailService emailService;

    private JavaMailSender mockSender;
    private MimeMessage mockMimeMessage;

    @BeforeEach
    void setUp() {
        mockSender = mock(JavaMailSender.class);
        mockMimeMessage = mock(MimeMessage.class);
        // lenient: some tests intentionally make the factory throw, so mockSender stubs won't be used
        lenient().when(mailSenderFactory.createMailSender(any())).thenReturn(mockSender);
        lenient().when(mockSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EmailProxyRequest plainTextRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("Test Subject")
                .text("Plain text body")
                .build();
    }

    private EmailProxyRequest htmlRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("HTML Subject")
                .text("Plain text body")
                .html("<h1>HTML body</h1>")
                .build();
    }

    private EmailProxyRequest icalRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("iCal Subject")
                .text("Plain text body")
                .icalEvent("BEGIN:VCALENDAR\nMETHOD:REQUEST\nEND:VCALENDAR")
                .build();
    }

    private EmailProxyRequest fullRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("Full Subject")
                .text("Plain text body")
                .html("<h1>HTML body</h1>")
                .icalEvent("BEGIN:VCALENDAR\nMETHOD:REQUEST\nEND:VCALENDAR")
                .build();
    }

    // ── Tests: plain text ─────────────────────────────────────────────────────

    @Test
    @DisplayName("plain text email: factory is called once")
    void plainText_factoryCalledOnce() {
        emailService.sendEmail(plainTextRequest());
        verify(mailSenderFactory, times(1)).createMailSender(any());
    }

    @Test
    @DisplayName("plain text email: sender.send() is called once")
    void plainText_senderCalledOnce() {
        emailService.sendEmail(plainTextRequest());
        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("plain text email: createMimeMessage() is called once")
    void plainText_createMimeMessageCalledOnce() {
        emailService.sendEmail(plainTextRequest());
        verify(mockSender, times(1)).createMimeMessage();
    }

    // ── Tests: HTML email ─────────────────────────────────────────────────────

    @Test
    @DisplayName("HTML email: sender.send() is called once")
    void html_senderCalledOnce() {
        emailService.sendEmail(htmlRequest());
        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("HTML email: factory is called once")
    void html_factoryCalledOnce() {
        emailService.sendEmail(htmlRequest());
        verify(mailSenderFactory, times(1)).createMailSender(any());
    }

    // ── Tests: iCal email ─────────────────────────────────────────────────────

    @Test
    @DisplayName("iCal email: sender.send() is called once")
    void ical_senderCalledOnce() {
        emailService.sendEmail(icalRequest());
        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("iCal email: factory is called once")
    void ical_factoryCalledOnce() {
        emailService.sendEmail(icalRequest());
        verify(mailSenderFactory, times(1)).createMailSender(any());
    }

    // ── Tests: full (HTML + iCal) email ───────────────────────────────────────

    @Test
    @DisplayName("full email (html + ical): sender.send() is called once")
    void full_senderCalledOnce() {
        emailService.sendEmail(fullRequest());
        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("full email (html + ical): factory is called once")
    void full_factoryCalledOnce() {
        emailService.sendEmail(fullRequest());
        verify(mailSenderFactory, times(1)).createMailSender(any());
    }

    // ── Tests: HTML-only (no text fallback) ──────────────────────────────────

    @Test
    @DisplayName("html-only email (text=null): sender.send() is called once")
    void htmlOnly_senderCalledOnce() {
        EmailProxyRequest req = EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("HTML only")
                .html("<h1>Hello</h1>")
                .build();
        emailService.sendEmail(req);
        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }


    @Test
    @DisplayName("MessagingException from sender is swallowed (fire-and-forget)")
    void messagingException_swallowed() {
        doThrow(new org.springframework.mail.MailSendException("SMTP failure"))
                .when(mockSender).send(any(MimeMessage.class));

        // Must not propagate
        assertThatNoException().isThrownBy(() -> emailService.sendEmail(plainTextRequest()));
    }

    @Test
    @DisplayName("RuntimeException from factory is swallowed (fire-and-forget)")
    void runtimeException_swallowed() {
        when(mailSenderFactory.createMailSender(any()))
                .thenThrow(new RuntimeException("Network unavailable"));

        assertThatNoException().isThrownBy(() -> emailService.sendEmail(plainTextRequest()));
    }

    @Test
    @DisplayName("on send failure, sender.send() is still called (no short-circuit before send)")
    void onSendFailure_sendWasCalled() {
        doThrow(new org.springframework.mail.MailSendException("SMTP failure"))
                .when(mockSender).send(any(MimeMessage.class));

        emailService.sendEmail(plainTextRequest());

        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("on factory failure, sender.send() is never called")
    void onFactoryFailure_sendNeverCalled() {
        when(mailSenderFactory.createMailSender(any()))
                .thenThrow(new RuntimeException("Factory error"));

        emailService.sendEmail(plainTextRequest());

        verify(mockSender, never()).send(any(MimeMessage.class));
    }

    // ── Tests: correct request forwarded to factory ───────────────────────────

    @Test
    @DisplayName("the exact request is forwarded to the factory")
    void requestForwardedToFactory() {
        EmailProxyRequest request = plainTextRequest();
        ArgumentCaptor<EmailProxyRequest> captor = ArgumentCaptor.forClass(EmailProxyRequest.class);

        emailService.sendEmail(request);

        verify(mailSenderFactory).createMailSender(captor.capture());
        assertThat(captor.getValue()).isSameAs(request);
    }

    @Test
    @DisplayName("MessagingException from MimeMessage setup is caught and swallowed (fire-and-forget)")
    void messagingExceptionFromMimeMessage_swallowed() throws Exception {
        // MimeMessageHelper calls setContent() on the MimeMessage during multipart construction.
        // Configuring the mock to throw MessagingException from setContent triggers the
        // catch (MessagingException) branch in EmailService.
        doThrow(new MessagingException("Mime setup failed"))
                .when(mockMimeMessage).setContent(any());

        // Use an HTML request so MimeMessageHelper uses multipart mode and calls setContent
        assertThatNoException().isThrownBy(() -> emailService.sendEmail(htmlRequest()));
    }
}
