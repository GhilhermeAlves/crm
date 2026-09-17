package com.becommerce.crm.presentation.rest.omnichannel;

import com.becommerce.crm.application.omnichannel.port.input.WhatsAppWebhookUseCase;
import com.becommerce.crm.infrastructure.omnichannel.whatsapp.WhatsAppWebhookTokenVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/omnichannel/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppWebhookUseCase webhookUseCase;
    private final WhatsAppWebhookSignatureVerifier signatureVerifier;
    private final WhatsAppWebhookTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(WhatsAppWebhookUseCase webhookUseCase,
                                     WhatsAppWebhookSignatureVerifier signatureVerifier,
                                     WhatsAppWebhookTokenVerifier tokenVerifier,
                                     ObjectMapper objectMapper) {
        this.webhookUseCase = webhookUseCase;
        this.signatureVerifier = signatureVerifier;
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
    }

    /** Handshake de verificação (GET). Retorna o challenge se o token conferir. */
    @GetMapping
    public ResponseEntity<String> verify(@RequestParam Map<String, String> params) {
        String challenge = webhookUseCase.verify(params);
        return challenge == null ? ResponseEntity.status(403).build() : ResponseEntity.ok(challenge);
    }

    /**
     * Evento recebido (mensagem ou status). Idempotente.
     * Aceito se o HMAC SHA-256 da Meta (X-Hub-Signature-256) OR o token
     * do UAZAPI (query {@code token} ou header X-Uazapi-Token) forem válidos.
     */
    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(value = "X-Uazapi-Token", required = false) String uazapiToken,
            @RequestBody String rawPayload) {
        if (!signatureVerifier.isValid(signature, rawPayload)
                && !tokenVerifier.isAuthenticated(rawPayload, signature, token != null ? token : uazapiToken)) {
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
