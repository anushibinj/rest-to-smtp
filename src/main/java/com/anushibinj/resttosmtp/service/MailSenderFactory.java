package com.anushibinj.resttosmtp.service;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Stateless factory that builds a fresh {@link JavaMailSender} for every request.
 *
 * <p><strong>Why no caching?</strong> Because this proxy accepts arbitrary SMTP credentials
 * in each request, caching senders introduces credential-leakage risk (one caller's session
 * returned to a different caller) and stale-password issues when credentials are rotated.
 * The overhead of constructing a new {@code JavaMailSenderImpl} is negligible compared with
 * the actual SMTP handshake, so there is no meaningful performance regression.
 *
 * <p>Connection-level timeouts are applied defensively so that a slow or unresponsive SMTP
 * server never holds a virtual thread beyond its useful lifetime.
 */
@Slf4j
@Component
public class MailSenderFactory {

    /** Connect timeout in milliseconds. */
    private static final String CONNECTION_TIMEOUT_MS = "5000";

    /** Socket read/write timeout in milliseconds. */
    private static final String IO_TIMEOUT_MS = "10000";

    /**
     * Creates a fully configured {@link JavaMailSender} using the credentials embedded
     * in {@code request}.
     *
     * @param request the inbound proxy request containing SMTP host, port, username and password
     * @return a ready-to-use {@link JavaMailSender}
     */
    public JavaMailSender createMailSender(EmailProxyRequest request) {
        log.debug("Building JavaMailSender for host={} port={} user={}",
                request.getSmtpHost(), request.getSmtpPort(), request.getSmtpUsername());

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(request.getSmtpHost());
        sender.setPort(request.getSmtpPort());
        sender.setUsername(request.getSmtpUsername());
        sender.setPassword(request.getSmtpPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "false"); // graceful fallback if server doesn't support STARTTLS
        props.put("mail.smtp.connectiontimeout", CONNECTION_TIMEOUT_MS);
        props.put("mail.smtp.timeout", IO_TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", IO_TIMEOUT_MS);
        // Improves deliverability: send EHLO with the correct local hostname
        props.put("mail.smtp.localhost", request.getSmtpHost());

        return sender;
    }
}
