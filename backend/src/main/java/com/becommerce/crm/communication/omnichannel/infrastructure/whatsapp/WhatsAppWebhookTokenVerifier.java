package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autenticação do webhook da Evolution API por token compartilhado (fail-safe):
 * <ul>
 *   <li>token configurado ({@code omnichannel.whatsapp.webhook-token}):
 *       o payload SÓ é aceito com o token correto (query param ou header);</li>
 *   <li>token vazio: aceita apenas em modo dev explícito
 *       ({@code omnichannel.whatsapp.webhook-allow-unsigned=true} — default é rejeitar).</li>
 * </ul>
 */
@Component
public class WhatsAppWebhookTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookTokenVerifier.class);

    private final boolean allowUnsigned;
    private final String webhookToken;

    public WhatsAppWebhookTokenVerifier(
            @Value("${omnichannel.whatsapp.webhook-allow-unsigned:false}") boolean allowUnsigned,
            @Value("${omnichannel.whatsapp.webhook-token:}") String webhookToken) {
        this.allowUnsigned = allowUnsigned;
        this.webhookToken = webhookToken == null ? "" : webhookToken.trim();
    }

    /** @param token token recebido (query param {@code token} ou header {@code X-Webhook-Token}) */
    public boolean isAuthenticated(String token) {
        if (!webhookToken.isEmpty()) {
            if (token == null) {
                log.warn("Webhook WhatsApp sem token; rejeitando");
                return false;
            }
            boolean ok = MessageDigest.isEqual(
                    webhookToken.getBytes(StandardCharsets.UTF_8),
                    token.trim().getBytes(StandardCharsets.UTF_8));
            if (!ok) {
                log.warn("Webhook WhatsApp com token inválido; rejeitando");
            }
            return ok;
        }
        if (!allowUnsigned) {
            log.warn("Webhook sem token configurado e allow-unsigned desativado; rejeitando");
            return false;
        }
        return true;
    }
}
