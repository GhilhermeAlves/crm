package com.becommerce.crm.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Provider de desenvolvimento: apenas loga o convite (nenhum e-mail real).
 * O token aparece no log para permitir o E2E/dev local.
 */
@Component
@Profile("!production")
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendInvitation(String to, String companyName, String role, String tokenUrl) {
        log.info("[ConsoleEmailSender] Convite para {} (empresa={}, role={}): {}", to, companyName, role, tokenUrl);
    }
}