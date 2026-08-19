package com.trio.backend.service;

import com.trio.backend.config.MailProperties;
import com.trio.backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Implementation of the service d'sending d'emails de Collabix.
 *
 * <p>Cette class utilise {@link JavaMailSender} pour send des emails
 * transactionals au format HTML. Each method est asynchronous et
 * transactionalle en lecture seule in order to ne pas bloquer le thread
 * main et de ne pas interfÃ©rer avec les transactions en Ã©criture.</p>
 *
 * <p><strong>Responsibilitys :</strong></p>
 * <ul>
 *     <li>Construire les messages MIME au format HTML.</li>
 *     <li>Envoyer les emails de maniÃ¨re asynchronous via Spring {@link Async}.</li>
 *     <li>Logger each sending pour faciliter le debugging et l'audit.</li>
 *     <li>Capturer et logger les errors d'sending sans les propager
 *     (failure non-blocking pour le flow main).</li>
 * </ul>
 *
 * <p><strong>Collaborators :</strong></p>
 * <ul>
 *     <li>{@link JavaMailSender} â€” driver SMTP configured via Spring Boot.</li>
 *     <li>{@link MailProperties} â€” settings de l'sender externalized.</li>
 * </ul>
 *
 * @see EmailService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    private final MailProperties mailProperties;

    private final RestClient restClient;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private record BrevoSender(String email, String name) {}

    private record BrevoRecipient(String email) {}

    private record BrevoEmailRequest(BrevoSender sender, List<BrevoRecipient> to, String subject, String htmlContent) {}

    /**
     * Envoie un email of activation de compte Ã  the user.
     *
     * <p>Construit un email HTML containing :</p>
     * <ul>
     *     <li>Le prÃ©name de the user dans le body du message.</li>
     *     <li>Une explanation indicating que le compte a Ã©tÃ© created par un administrator.</li>
     *     <li>Un goalton "Activer mon compte" pointing vers le link of activation.</li>
     *     <li>Le link of activation brut pour les clinkts email qui n'affichent pas les goaltons.</li>
     * </ul>
     *
     * @param user           the user recipient de l'email
     * @param activationLink le link of activation complete Ã  inclure dans l'email
     */
    @Override
    @Async
    public void sendAccountActivationEmail(User user, String activationLink) {

        String subject = "Activez votre compte Collabix";

        String htmlContent = buildActivationHtml(user.getFirstName(), activationLink);

        try {

            sendEmail(user.getEmail(), subject, htmlContent);

            log.info("Account activation email sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send account activation email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Envoie un email de reset de mot de passe Ã  the user.
     *
     * <p>Construit un email HTML containing :</p>
     * <ul>
     *     <li>Le prÃ©name de the user dans le body du message.</li>
     *     <li>Une explanation indicating qu'une request de reset a Ã©tÃ© performed.</li>
     *     <li>Un goalton "RÃ©initialize mon mot de passe" pointing vers le link.</li>
     *     <li>Le link brut pour les clinkts email qui n'affichent pas les goaltons.</li>
     *     <li>Un warning security si the user is not Ã  l'origine de la request.</li>
     * </ul>
     *
     * @param user     the user recipient de l'email
     * @param resetLink le link de reset complete Ã  inclure dans l'email
     */
    @Override
    @Async
    public void sendPasswordResetEmail(User user, String resetLink) {

        String subject = "Reset de votre mot de passe Collabix";

        String htmlContent = buildPasswordResetHtml(user.getFirstName(), resetLink);

        try {

            sendEmail(user.getEmail(), subject, htmlContent);

            log.info("Password reset email sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Routes the email: via the Brevo HTTP API when BREVO_API_KEY is set,
     * otherwise via the configured SMTP (JavaMailSender).
     */
    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        if (mailProperties.getBrevoApiKey() != null && !mailProperties.getBrevoApiKey().isBlank()) {
            sendViaBrevo(to, subject, htmlContent);
        } else {
            sendViaSmtp(to, subject, htmlContent);
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private void sendViaBrevo(String to, String subject, String htmlContent) {
        BrevoEmailRequest request = new BrevoEmailRequest(
                new BrevoSender(mailProperties.getFrom(), mailProperties.getFromName()),
                List.of(new BrevoRecipient(to)),
                subject,
                htmlContent
        );
        restClient.post()
                .uri(BREVO_API_URL)
                .header("api-key", mailProperties.getBrevoApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.debug("Brevo API accepted email to: {}", to);
    }

    /**
     * Construit le body HTML de l'email of activation.
     *
     * @param firstName      le prÃ©name de the user
     * @param activationLink le link of activation complete
     * @return une chain HTML formattede
     */
    private String buildActivationHtml(String firstName, String activationLink) {

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Activation de votre compte Collabix</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f4f4f4; font-family: Arial, Helvetica, sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 40px 0 20px 0; background-color: #1a73e8; border-radius: 8px 8px 0 0;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">Collabix</h1>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                Bonjour <strong style="color: #1a73e8;">%s</strong>,
                                            </p>
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                Votre compte Collabix a Ã©tÃ© created par un administrator. Pour finaliser
                                                creation de votre compte et accÃ©der Ã  la plateforme, veuillez cliquer
                                                sur le goalton ci-dessous pour l'activer.
                                            </p>
                                            <!-- Goalton -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                <tr>
                                                    <td align="center">
                                                        <table role="presentation" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td align="center" style="background-color: #1a73e8; border-radius: 6px;">
                                                                    <a href="%s"
                                                                       target="_blank"
                                                                       style="display: inline-block; padding: 14px 40px; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; border-radius: 6px;">
                                                                        Activer mon compte
                                                                    </a>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                            <!-- Raw link fallback -->
                                            <p style="color: #666666; font-size: 14px; line-height: 1.5; margin: 20px 0 0 0;">
                                                Si le goalton ne functionne pas, copiez et collez le link following
                                                dans votre navigateur :
                                            </p>
                                            <p style="color: #1a73e8; font-size: 14px; line-height: 1.5; word-break: break-all; margin: 10px 0 0 0;">
                                                <a href="%s" target="_blank" style="color: #1a73e8;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 20px 30px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                                            <p style="color: #999999; font-size: 12px; line-height: 1.5; margin: 0; text-align: center;">
                                                Cet email a Ã©tÃ© sent automaticment par Collabix. Merci de ne pas y rÃ©pondre.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(firstName, activationLink, activationLink, activationLink);
    }

    /**
     * Construit le body HTML de l'email de reset de mot de passe.
     *
     * @param firstName le prÃ©name de the user
     * @param resetLink le link de reset complete
     * @return une chain HTML formattede
     */
    @Override
    @Async
    public void sendNotificationEmail(User user, String notificationTitle, String notificationBody, String actionLink) {

        String subject = notificationTitle;

        String htmlContent = buildNotificationHtml(user.getFirstName(), notificationTitle, notificationBody, actionLink);

        try {

            sendEmail(user.getEmail(), subject, htmlContent);

            log.info("Notification email sent successfully to: {} — title: {}", user.getEmail(), notificationTitle);

        } catch (Exception e) {
            log.error("Failed to send notification email to: {}", user.getEmail(), e);
        }
    }

    private String buildPasswordResetHtml(String firstName, String resetLink) {

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reset de votre mot de passe Collabix</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f4f4f4; font-family: Arial, Helvetica, sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 40px 0 20px 0; background-color: #1a73e8; border-radius: 8px 8px 0 0;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">Collabix</h1>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                Bonjour <strong style="color: #1a73e8;">%s</strong>,
                                            </p>
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                Vous avez demandÃ© la reset de votre mot de passe Collabix.
                                                Pour dÃ©finir a new mot de passe, veuillez cliquer sur le goalton
                                                ci-dessous.
                                            </p>
                                            <!-- Goalton -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                <tr>
                                                    <td align="center">
                                                        <table role="presentation" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td align="center" style="background-color: #1a73e8; border-radius: 6px;">
                                                                    <a href="%s"
                                                                       target="_blank"
                                                                       style="display: inline-block; padding: 14px 40px; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; border-radius: 6px;">
                                                                        RÃ©initialize mon mot de passe
                                                                    </a>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                            <!-- Raw link fallback -->
                                            <p style="color: #666666; font-size: 14px; line-height: 1.5; margin: 20px 0 0 0;">
                                                Si le goalton ne functionne pas, copiez et collez le link following
                                                dans votre navigateur :
                                            </p>
                                            <p style="color: #1a73e8; font-size: 14px; line-height: 1.5; word-break: break-all; margin: 10px 0 0 0;">
                                                <a href="%s" target="_blank" style="color: #1a73e8;">%s</a>
                                            </p>
                                            <!-- Security warning -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0 0 0; background-color: #fef2f2; border-radius: 6px; border: 1px solid #fecaca;">
                                                <tr>
                                                    <td style="padding: 14px 20px;">
                                                        <p style="color: #991b1b; font-size: 13px; line-height: 1.6; margin: 0;">
                                                            <strong>Vous n'avez pas demandÃ© cette reset ?</strong>
                                                            Si vous n'Ãªtes pas Ã  l'origine de cette request, ignorez cet email
                                                            et votre mot de passe restera inchangÃ©.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 20px 30px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                                            <p style="color: #999999; font-size: 12px; line-height: 1.5; margin: 0; text-align: center;">
                                                Cet email a Ã©tÃ© sent automaticment par Collabix. Merci de ne pas y rÃ©pondre.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(firstName, resetLink, resetLink, resetLink);
    }

    private String buildNotificationHtml(String firstName, String title, String body, String actionLink) {
        String bodySection = (body != null && !body.isBlank())
                ? "<p style=\"color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;\">%s</p>".formatted(body)
                : "";

        String goaltonSection = (actionLink != null && !actionLink.isBlank())
                ? """
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" cellpadding="0" cellspacing="0">
                                    <tr>
                                        <td align="center" style="background-color: #1a73e8; border-radius: 6px;">
                                            <a href="%s"
                                               target="_blank"
                                               style="display: inline-block; padding: 14px 40px; color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 600; border-radius: 6px;">
                                                View Details
                                            </a>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                    <p style="color: #666666; font-size: 14px; line-height: 1.5; margin: 20px 0 0 0;">
                        Si le goalton ne functionne pas, copiez et collez le link following dans votre navigateur :
                    </p>
                    <p style="color: #1a73e8; font-size: 14px; line-height: 1.5; word-break: break-all; margin: 10px 0 0 0;">
                        <a href="%s" target="_blank" style="color: #1a73e8;">%s</a>
                    </p>
                    """.formatted(actionLink, actionLink, actionLink)
                : "";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f4f4f4; font-family: Arial, Helvetica, sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td align="center" style="padding: 40px 0 20px 0; background-color: #1a73e8; border-radius: 8px 8px 0 0;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">Collabix Notification</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                Bonjour <strong style="color: #1a73e8;">%s</strong>,
                                            </p>
                                            <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                                <strong>%s</strong>
                                            </p>
                                            %s
                                            %s
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 20px 30px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                                            <p style="color: #999999; font-size: 12px; line-height: 1.5; margin: 0; text-align: center;">
                                                Cet email a Ã©tÃ© sent automaticment par Collabix. Merci de ne pas y rÃ©pondre.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(title, firstName, title, bodySection, goaltonSection);
    }
}

