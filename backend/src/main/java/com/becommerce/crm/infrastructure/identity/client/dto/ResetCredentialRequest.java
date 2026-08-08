package com.becommerce.crm.infrastructure.identity.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Corpo do {@code POST /internal/auth/reset-password} (Sprint 7.4) chamado no
 * crm-auth-service. A nova senha é texto puro — via corpo HTTP somente, nunca
 * logada.
 */
public record ResetCredentialRequest(
        @JsonProperty("keycloakSub") String keycloakSub,
        String email,
        String newPassword) {
}