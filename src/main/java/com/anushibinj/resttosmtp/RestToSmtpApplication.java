package com.anushibinj.resttosmtp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the REST-to-SMTP proxy microservice.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>{@code @EnableAsync} activates Spring's asynchronous method execution,
 *       paired with our virtual-thread executor in {@link com.anushibinj.resttosmtp.config.AsyncConfig}.</li>
 *   <li>{@code MailSenderAutoConfiguration} is excluded because we instantiate
 *       {@code JavaMailSenderImpl} dynamically per request in
 *       {@link com.anushibinj.resttosmtp.service.MailSenderFactory} — no static SMTP
 *       credentials live in {@code application.yml}.</li>
 * </ul>
 */
@SpringBootApplication(exclude = {MailSenderAutoConfiguration.class})
@EnableAsync
public class RestToSmtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestToSmtpApplication.class, args);
    }
}
