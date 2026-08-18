package com.becommerce.crm.infrastructure.identity.email;

import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.notification.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Provider de e-mail real via SMTP (JavaMail). Ativo quando
 * {@code app.mail.provider=smtp} (env {@code MAIL_PROVIDER=smtp}).
 *
 * <p>Implementa tanto {@link EmailSender} (convites) quanto {@link EmailService}
 * (reset de senha). Configuração SMTP feita via {@code spring.mail.*} (host,
 * porta, username/password) sem segredos no Git.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender, EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendBaseUrl;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${app.mail.from:}") String from,
                           @Value("${app.mail.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void sendInvitation(String to, String companyName, String role, String tokenUrl) {
        String subject = "Você foi convidado para " + companyName;
        String body = """
                <p>Olá!</p>
                <p>Você foi convidado para ingressar na empresa <strong>%s</strong>
                com o papel <strong>%s</strong>.</p>
                <p><a href="%s">Aceitar convite</a></p>
                <p>Se não foi você, ignore este e-mail.</p>
                """.formatted(companyName, role, tokenUrl);
        sendHtml(to, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        String subject = "Redefinição de senha";
        String body = """
                <p>Olá!</p>
                <p>Recebemos uma solicitação para redefinir sua senha.</p>
                <p><a href="%s">Redefinir minha senha</a></p>
                <p>Se não foi você, ignore este e-mail.</p>
                """.formatted(link);
        sendHtml(to, subject, body);
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("E-mail enviado para {} (assunto: {})", to, subject);
        } catch (MessagingException e) {
            log.error("Falha ao enviar e-mail para {}: {}", to, e.getMessage());
            throw new IllegalStateException("Falha ao enviar e-mail para " + to, e);
        }
    }
}
