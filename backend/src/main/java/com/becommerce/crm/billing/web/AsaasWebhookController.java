package com.becommerce.crm.billing.web;

import com.becommerce.crm.billing.application.AsaasWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Webhook público do Asaas. Autenticação pelo header asaas-access-token. */
@RestController
@RequestMapping("/api/v1/public/billing/asaas/webhook")
public class AsaasWebhookController {

    private final AsaasWebhookService service;

    public AsaasWebhookController(AsaasWebhookService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody JsonNode body) {
        if (!service.tokenMatches(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("received", false));
        }
        service.handle(body);
        return ResponseEntity.ok(Map.of("received", true));
    }
}
