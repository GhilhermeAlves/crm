package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.MetaWhatsAppWebhookParser;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook da Evolution API. Configurar na instância a URL
 * {@code http://backend:8080/api/v1/omnichannel/whatsapp/webhook?token=<OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN>}
 * (ou o header {@code X-Webhook-Token}). Na Meta, a Callback URL usa o mesmo
 * {@code ?token=} e o Verify Token é {@code META_WEBHOOK_VERIFY_TOKEN}.
 */
@RestController
@RequestMapping("/api/v1/omnichannel/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppWebhookUseCase webhookUseCase;
    private final WhatsAppWebhookTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(WhatsAppWebhookUseCase webhookUseCase,
                                     WhatsAppWebhookTokenVerifier tokenVerifier,
                                     ObjectMapper objectMapper) {
        this.webhookUseCase = webhookUseCase;
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
    }

    /**
     * Handshake de verificação da Meta: responde o {@code hub.challenge} em texto
     * puro quando {@code hub.mode=subscribe} e o verify token confere.
     */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (challenge == null || !tokenVerifier.isMetaSubscription(mode, verifyToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
    }

    /**
     * Evento recebido (mensagem ou status). Idempotente. Exige o token do webhook;
     * payloads da Meta exigem também a assinatura {@code X-Hub-Signature-256}.
     */
    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(value = "X-Webhook-Token", required = false) String headerToken,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String metaSignature,
            @RequestBody byte[] rawPayload) {
        if (!tokenVerifier.isAuthenticated(token != null ? token : headerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawPayload, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Webhook WhatsApp com payload inválido; rejeitando");
            return ResponseEntity.badRequest().build();
        }
        // A assinatura é sobre os bytes exatos recebidos, por isso o corpo chega como byte[].
        if (MetaWhatsAppWebhookParser.isMetaPayload(payload)
                && !tokenVerifier.isValidMetaSignature(rawPayload, metaSignature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        webhookUseCase.handleEvent(payload);
        return ResponseEntity.ok().build();
    }
}
