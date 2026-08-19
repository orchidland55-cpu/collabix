package com.trio.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the email sending system.
 *
 * <p>All values are externalized via environment variables with sensible defaults
 * for development. To switch SMTP provider, simply change the environment variables
 * without modifying any code.</p>
 *
 * <p><strong>Compatible SMTP providers:</strong></p>
 * <ul>
 *     <li><strong>Gmail:</strong> host=smtp.gmail.com, port=587, TLS enabled</li>
 *     <li><strong>Outlook:</strong> host=smtp.office365.com, port=587, TLS enabled</li>
 *     <li><strong>SendGrid:</strong> host=smtp.sendgrid.net, port=587, TLS enabled, username=apikey</li>
 *     <li><strong>AWS SES:</strong> host=email-smtp.<region>.amazonaws.com, port=587, TLS enabled</li>
 *     <li><strong>Mailgun:</strong> host=smtp.mailgun.org, port=587, TLS enabled</li>
 * </ul>
 *
 * <p>Usage in services:</p>
 * <pre>{@code
 * @Component
 * @ConfigurationProperties(prefix = "app.mail")
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

    /**
     * Sender email address (e.g., noreply@collabix.app).
     * <p>Environment variable: {@code MAIL_FROM_ADDRESS}</p>
     */
    private String from;

    /**
     * Sender display name (e.g., Collabix).
     * <p>Environment variable: {@code MAIL_FROM_NAME}</p>
     */
    private String fromName;

    /**
     * Brevo REST API key (xkeysib-...). When set, emails are sent via the
     * Brevo HTTP API (port 443) instead of SMTP, bypassing any egress SMTP
     * restrictions.
     * <p>Environment variable: {@code BREVO_API_KEY}</p>
     */
    private String brevoApiKey;
}
