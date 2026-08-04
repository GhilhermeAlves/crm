package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.infrastructure.health.DependencyProbe;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Healthcheck do Access Gateway (Sprint 6.6), separando liveness de readiness.
 *
 * <ul>
 *   <li>{@code GET /auth/health} — <b>liveness</b>: o processo está vivo.
 *       Não depende de Keycloak, Redis, backend ou login real (resposta
 *       mínima e pública).</li>
 *   <li>{@code GET /auth/health/ready} — <b>readiness</b>: dependências
 *       críticas para operar o Gateway (Redis e descoberta OIDC do Keycloak)
 *       estão disponíveis. {@code 200 UP} / {@code 503 DOWN} com detalhes
 *       mínimos e seguros (apenas UP/DOWN por dependência — nunca
 *       host/porta/segredo/stack).</li>
 * </ul>
 */
@RestController
public class HealthController {

    private final DependencyProbe dependencyProbe;

    public HealthController(DependencyProbe dependencyProbe) {
        this.dependencyProbe = dependencyProbe;
    }

    @GetMapping("/auth/health")
    public Map<String, String> liveness() {
        return Map.of("status", "UP");
    }

    @GetMapping("/auth/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        boolean redisUp = dependencyProbe.redisReachable();
        boolean keycloakUp = dependencyProbe.keycloakReachable();
        boolean ready = redisUp && keycloakUp;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ready ? "UP" : "DOWN");
        body.put("checks", Map.of(
                "redis", redisUp ? "UP" : "DOWN",
                "keycloak", keycloakUp ? "UP" : "DOWN"));

        return ready
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
