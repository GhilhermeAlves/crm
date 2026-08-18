package com.becommerce.crm.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Provider atual de e-mail: apenas registra no log (nenhum e-mail real é
 * enviado). Disponível quando o SMTP não está ativo
 * ({@code app.mail.provider != smtp}). Em produção, usar
 * {@code app.mail.provider=smtp} para enviar via {@code SmtpEmailSender}
 * (SMTP/Resend/SES) mantendo a interface {@link EmailSender} — sem segredos no
 * Git.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendInvitation(String to, String companyName, String role, String tokenUrl) {
        log.info("[ConsoleEmailSender] Convite para {} (empresa={}, role={}): {}", to, companyName, role, tokenUrl);
    }
}