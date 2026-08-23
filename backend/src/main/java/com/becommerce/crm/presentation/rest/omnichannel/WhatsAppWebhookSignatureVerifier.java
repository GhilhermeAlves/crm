package com.becommerce.crm.presentation.rest.omnichannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validação HMAC SHA-256 do webhook WhatsApp, no padrão Meta
 * (header {@code X-Hub-Signature-256: sha256=<hex>} sobre o body bruto).
 *
 * <p>Regras:
 * <ul>
 *   <li>Com {@code omnichannel.whatsapp.app-secret} configurado: assinatura é
 *       OBRIGATÓRIA — inválida ou ausente rejeita a requisição.</li>
 *   <li>Sem secret: só aceita se {@code omnichannel.whatsapp.webhook-allow-unsigned=true}
 *       (modo de desenvolvimento explícito; default é rejeitar).</li>
 * </ul>
 * O secret e a assinatura completa nunca são logados.
 */
@Component
public class WhatsAppWebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookSignatureVerifier.class);
    private static final String PREFIX = "sha256=";

    private final String appSecret;
    private final boolean allowUnsigned;

    public WhatsAppWebhookSignatureVerifier(
            @Value("${omnichannel.whatsapp.app-secret:}") String appSecret,
            @Value("${omnichannel.whatsapp.webhook-allow-unsigned:false}") boolean allowUnsigned) {
        this.appSecret = appSecret == null ? "" : appSecret.trim();
        this.allowUnsigned = allowUnsigned;
    }

    public boolean isEnforced() {
        return !appSecret.isEmpty();
    }

    /** @return true se a requisição pode ser processada. */
    public boolean isValid(String signatureHeader, String rawPayload) {
        if (!isEnforced()) {
            if (!allowUnsigned) {
                log.warn("Webhook WhatsApp sem app-secret configurado e modo allow-unsigned desativado; rejeitando");
                return false;
            }
            return true;
        }
        if (signatureHeader == null || !signatureHeader.toLowerCase().startsWith(PREFIX)) {
            log.warn("Webhook WhatsApp sem assinatura X-Hub-Signature-256 válida; rejeitando");
            return false;
        }
        String expected = computeHmac(rawPayload);
        String provided = signatureHeader.substring(PREFIX.length()).trim().toLowerCase();
        boolean ok = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII));
        if (!ok) {
            log.warn("Assinatura X-Hub-Signature-256 inválida; rejeitando webhook");
        }
        return ok;
    }

    private String computeHmac(String rawPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC do webhook", e);
        }
    }
}
