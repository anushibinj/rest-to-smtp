package com.anushibinj.resttosmtp.dto;

import com.anushibinj.resttosmtp.dto.validation.AtLeastOneBodyPresent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stateless email proxy request payload.
 *
 * <p>Every request is fully self-contained: the caller supplies both the SMTP credentials
 * and the email payload in a single JSON body. No server-side session or credential store
 * is used, making the service trivially horizontally scalable.
 *
 * <p>Validation is enforced at the controller boundary via {@code @Valid}. Any constraint
 * violations are translated to a structured 400 response by
 * {@link com.anushibinj.resttosmtp.exception.GlobalExceptionHandler}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AtLeastOneBodyPresent
@Schema(
        name = "EmailProxyRequest",
        description = "Self-contained email send request including SMTP credentials and message payload"
)
public class EmailProxyRequest {

    // ── SMTP Connection ──────────────────────────────────────────────────────

    @NotBlank
    @Schema(
            description = "Hostname or IP address of the target SMTP server",
            example = "smtp.gmail.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String smtpHost;

    @NotNull
    @Min(value = 1, message = "smtpPort must be between 1 and 65535")
    @Max(value = 65535, message = "smtpPort must be between 1 and 65535")
    @Schema(
            description = "TCP port of the SMTP server (e.g. 587 for STARTTLS, 465 for SMTPS, 25 for plain)",
            example = "587",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer smtpPort;

    @NotBlank
    @Schema(
            description = "SMTP authentication username (often the sender email address)",
            example = "user@gmail.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String smtpUsername;

    @NotBlank
    @Schema(
            description = "SMTP authentication password or app-specific password",
            example = "s3cr3t-app-password",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String smtpPassword;

    // ── Message Envelope ─────────────────────────────────────────────────────

    @NotBlank
    @Email(message = "to must be a valid email address")
    @Schema(
            description = "Recipient email address",
            example = "recipient@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String to;

    @NotBlank
    @Email(message = "from must be a valid email address")
    @Schema(
            description = "Sender email address (must be authorised on the SMTP server)",
            example = "sender@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String from;

    @NotBlank
    @Schema(
            description = "Email subject line",
            example = "Meeting confirmation",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String subject;

    // ── Message Body ─────────────────────────────────────────────────────────

    @Schema(
            description = "Plain-text body. Either this or 'html' (or both) must be provided.",
            example = "Hi, your meeting is confirmed.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String text;

    @Schema(
            description = "HTML body. Either this or 'text' (or both) must be provided. "
                        + "When provided, the message is sent as multipart/alternative "
                        + "containing both the plain-text and HTML parts.",
            example = "<h1>Hi</h1><p>Your meeting is confirmed.</p>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String html;

    @Schema(
            description = "Optional iCalendar event string (BEGIN:VCALENDAR ... END:VCALENDAR). "
                        + "When provided, it is attached as 'invite.ics' with content-type "
                        + "'text/calendar; method=REQUEST', enabling calendar clients to parse "
                        + "the invite automatically.",
            example = "BEGIN:VCALENDAR\\nMETHOD:REQUEST\\n...\\nEND:VCALENDAR",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String icalEvent;
}
