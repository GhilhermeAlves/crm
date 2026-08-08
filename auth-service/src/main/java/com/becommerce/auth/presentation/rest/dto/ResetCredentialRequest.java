package com.becommerce.auth.presentation.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contrato de entrada do {@code POST /internal/auth/reset-password} (Sprint 7.4).
 * Chamado pelo crm-backend de serviço a serviço (header
 * {@code X-Internal-Api-Token}), com autenticação validada pelo
 * {@code InternalApiTokenFilter}. A nova senha é texto puro — nunca logada,
 * apenas encaminhada no corpo para o Keycloak.
 */
public record ResetCredentialRequest(
        @JsonProperty("keycloakSub") String keycloakSub,
        String email,
        String newPassword) {
}