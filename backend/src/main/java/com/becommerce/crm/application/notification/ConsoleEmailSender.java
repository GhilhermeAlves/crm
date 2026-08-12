package com.becommerce.crm.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provider atual de e-mail: apenas registra no log (nenhum e-mail real é
 * enviado). Disponível em todos os perfis para não quebrar o startup sem um
 * provider SMTP configurado. Em produção, trocar por um provider real
 * (SMTP/Resend/SES) mantendo a interface {@link EmailSender} — sem segredos no
 * Git.
 */
@Component
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendInvitation(String to, String companyName, String role, String tokenUrl) {
        log.info("[ConsoleEmailSender] Convite para {} (empresa={}, role={}): {}", to, companyName, role, tokenUrl);
    }
}