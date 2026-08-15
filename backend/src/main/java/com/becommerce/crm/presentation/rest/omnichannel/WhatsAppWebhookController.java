package com.becommerce.crm.presentation.rest.omnichannel;

import com.becommerce.crm.application.omnichannel.port.input.WhatsAppWebhookUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook da WhatsApp Cloud API (Meta) — FASE 6/10/17.
 * Público (sem JWT): a Meta não autentica no nosso recurso; a verificação usa
 * o hub.verify_token configurado e o processamento é idempotente.
 */
@RestController
@RequestMapping("/api/v1/omnichannel/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final WhatsAppWebhookUseCase webhookUseCase;

    public WhatsAppWebhookController(WhatsAppWebhookUseCase webhookUseCase) {
        this.webhookUseCase = webhookUseCase;
    }

    /** Handshake de verificação (GET). Retorna o challenge se o token conferir. */
    @GetMapping
    public ResponseEntity<String> verify(@RequestParam Map<String, String> params) {
        String challenge = webhookUseCase.verify(params);
        return challenge == null ? ResponseEntity.status(403).build() : ResponseEntity.ok(challenge);
    }

    /** Evento recebido (mensagem ou status). Idempotente. */
    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody Map<String, Object> payload) {
        webhookUseCase.handleEvent(payload);
        return ResponseEntity.ok().build();
    }
}
