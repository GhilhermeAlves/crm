package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

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
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final boolean allowUnsigned;
    private final String webhookToken;

    /** Verify token do handshake da Meta ({@code hub.verify_token}); vazio = handshake recusado. */
    private final String metaVerifyToken;

    /** App Secret da Meta: assina cada POST em {@code X-Hub-Signature-256}; vazio = POST da Meta recusado. */
    private final String metaAppSecret;

    @Autowired
    public WhatsAppWebhookTokenVerifier(
            @Value("${omnichannel.whatsapp.webhook-allow-unsigned:false}") boolean allowUnsigned,
            @Value("${omnichannel.whatsapp.webhook-token:}") String webhookToken,
            @Value("${omnichannel.whatsapp.meta.verify-token:}") String metaVerifyToken,
            @Value("${omnichannel.whatsapp.meta.app-secret:}") String metaAppSecret) {
        this.allowUnsigned = allowUnsigned;
        this.webhookToken = webhookToken == null ? "" : webhookToken.trim();
        this.metaVerifyToken = metaVerifyToken == null ? "" : metaVerifyToken.trim();
        this.metaAppSecret = metaAppSecret == null ? "" : metaAppSecret.trim();
    }

    public WhatsAppWebhookTokenVerifier(boolean allowUnsigned, String webhookToken) {
        this(allowUnsigned, webhookToken, "", "");
    }

    /**
     * Assinatura da Meta: {@code sha256=<hex>} = HMAC-SHA256 do corpo bruto com o App Secret.
     * Sem secret configurado, só passa em modo dev ({@code webhook-allow-unsigned=true}).
     */
    public boolean isValidMetaSignature(byte[] rawBody, String signatureHeader) {
        if (metaAppSecret.isEmpty()) {
            if (!allowUnsigned) {
                log.warn("Webhook da Meta sem META_APP_SECRET configurado; rejeitando");
            }
            return allowUnsigned;
        }
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Webhook da Meta sem X-Hub-Signature-256; rejeitando");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(metaAppSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(rawBody)).getBytes(StandardCharsets.US_ASCII);
            byte[] received = signatureHeader.substring(SIGNATURE_PREFIX.length()).trim()
                    .toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
            boolean ok = MessageDigest.isEqual(expected, received);
            if (!ok) {
                log.warn("Webhook da Meta com assinatura inválida; rejeitando");
            }
            return ok;
        } catch (GeneralSecurityException e) {
            log.error("Falha ao calcular a assinatura do webhook da Meta", e);
            return false;
        }
    }

    /** Handshake GET da Meta: {@code hub.mode=subscribe} e verify token correto. */
    public boolean isMetaSubscription(String mode, String verifyToken) {
        if (!"subscribe".equals(mode) || metaVerifyToken.isEmpty() || verifyToken == null) {
            log.warn("Verificação de webhook da Meta recusada");
            return false;
        }
        boolean ok = MessageDigest.isEqual(
                metaVerifyToken.getBytes(StandardCharsets.UTF_8),
                verifyToken.trim().getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            log.warn("Verificação de webhook da Meta com verify token inválido");
        }
        return ok;
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
