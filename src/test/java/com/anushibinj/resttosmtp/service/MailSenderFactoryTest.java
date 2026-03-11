package com.anushibinj.resttosmtp.service;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MailSenderFactory}.
 *
 * <p>Verifies that the factory constructs a correctly configured {@link JavaMailSenderImpl}
 * from a given {@link EmailProxyRequest}. No live SMTP connection is made.
 */
@DisplayName("MailSenderFactory")
class MailSenderFactoryTest {

    private MailSenderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new MailSenderFactory();
    }

    private EmailProxyRequest baseRequest() {
        return EmailProxyRequest.builder()
                .smtpHost("smtp.example.com")
                .smtpPort(587)
                .smtpUsername("user@example.com")
                .smtpPassword("secret")
                .to("to@example.com")
                .from("from@example.com")
                .subject("Test Subject")
                .text("Hello")
                .build();
    }

    @Test
    @DisplayName("returns a non-null JavaMailSender")
    void returnsNonNull() {
        JavaMailSender sender = factory.createMailSender(baseRequest());
        assertThat(sender).isNotNull();
    }

    @Test
    @DisplayName("returned sender is a JavaMailSenderImpl")
    void returnsCorrectType() {
        JavaMailSender sender = factory.createMailSender(baseRequest());
        assertThat(sender).isInstanceOf(JavaMailSenderImpl.class);
    }

    @Test
    @DisplayName("sets host correctly")
    void setsHost() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getHost()).isEqualTo("smtp.example.com");
    }

    @Test
    @DisplayName("sets port correctly")
    void setsPort() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getPort()).isEqualTo(587);
    }

    @Test
    @DisplayName("sets username correctly")
    void setsUsername() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("sets password correctly")
    void setsPassword() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getPassword()).isEqualTo("secret");
    }

    @Test
    @DisplayName("sets default encoding to UTF-8")
    void setsEncoding() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getDefaultEncoding()).isEqualTo("UTF-8");
    }

    @Test
    @DisplayName("enables SMTP auth in JavaMail properties")
    void enablesSmtpAuth() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.auth")).isEqualTo("true");
    }

    @Test
    @DisplayName("enables STARTTLS in JavaMail properties")
    void enablesStartTls() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
    }

    @Test
    @DisplayName("sets transport protocol to smtp")
    void setsTransportProtocol() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getJavaMailProperties().getProperty("mail.transport.protocol")).isEqualTo("smtp");
    }

    @Test
    @DisplayName("sets connection timeout in JavaMail properties")
    void setsConnectionTimeout() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout")).isEqualTo("5000");
    }

    @Test
    @DisplayName("sets read timeout in JavaMail properties")
    void setsReadTimeout() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.timeout")).isEqualTo("10000");
    }

    @Test
    @DisplayName("sets write timeout in JavaMail properties")
    void setsWriteTimeout() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.createMailSender(baseRequest());
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.writetimeout")).isEqualTo("10000");
    }

    @Test
    @DisplayName("each call returns a new instance (stateless — no caching)")
    void returnsNewInstanceEachCall() {
        EmailProxyRequest request = baseRequest();
        JavaMailSender first = factory.createMailSender(request);
        JavaMailSender second = factory.createMailSender(request);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("different credentials produce independent instances")
    void differentCredentialsProduceIndependentInstances() {
        EmailProxyRequest req1 = baseRequest();
        EmailProxyRequest req2 = EmailProxyRequest.builder()
                .smtpHost("smtp.other.com")
                .smtpPort(465)
                .smtpUsername("other@other.com")
                .smtpPassword("other-secret")
                .to("t@example.com")
                .from("f@example.com")
                .subject("S")
                .text("T")
                .build();

        JavaMailSenderImpl s1 = (JavaMailSenderImpl) factory.createMailSender(req1);
        JavaMailSenderImpl s2 = (JavaMailSenderImpl) factory.createMailSender(req2);

        assertThat(s1.getHost()).isEqualTo("smtp.example.com");
        assertThat(s2.getHost()).isEqualTo("smtp.other.com");
        assertThat(s1.getPort()).isEqualTo(587);
        assertThat(s2.getPort()).isEqualTo(465);
    }
}
