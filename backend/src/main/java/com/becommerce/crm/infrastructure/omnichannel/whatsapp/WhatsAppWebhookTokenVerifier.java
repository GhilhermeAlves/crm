package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autenticação de webhook para providers que NÃO usam o HMAC da Meta
 * (ex.: UAZAPI). Segue a mesma política fail-safe do verifier HMAC:
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

    /**
     * @param rawPayload     body bruto (para futuro HMAC, mantido por assinatura)
     * @param signatureHeader header X-Hub-Signature-256 (Meta; hoje ignorado quando há token)
     * @param tokenParam      token recebido (query param {@code token} ou header X-Uazapi-Token)
     */
    public boolean isAuthenticated(String rawPayload, String signatureHeader, String tokenParam) {
        if (!webhookToken.isEmpty()) {
            if (tokenParam == null) {
                log.warn("Webhook {} sem token; rejeitando", "uazapi");
                return false;
            }
            boolean ok = MessageDigest.isEqual(
                    webhookToken.getBytes(StandardCharsets.UTF_8),
                    tokenParam.trim().getBytes(StandardCharsets.UTF_8));
            if (!ok) {
                log.warn("Webhook {} com token inválido; rejeitando", "uazapi");
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