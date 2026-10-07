package com.becommerce.crm.communication.omnichannel.web;

import com.becommerce.crm.communication.omnichannel.application.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * (ou o header {@code X-Webhook-Token}).
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

    /** Evento recebido (mensagem ou status). Idempotente. Exige o token do webhook. */
    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(value = "X-Webhook-Token", required = false) String headerToken,
            @RequestBody String rawPayload) {
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
        webhookUseCase.handleEvent(payload);
        return ResponseEntity.ok().build();
    }
}
