package com.becommerce.auth.presentation.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Healthcheck simples do serviço (aceite do Sprint 2: {@code GET /auth/health}).
 */
@RestController
public class HealthController {

    @GetMapping("/auth/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
