package com.becommerce.auth.presentation.rest.dto;

/**
 * Resposta do {@code POST /internal/auth/create-user} — contém apenas o ID do
 * usuário criado no Keycloak. NUNCA expõe senha, hash, admin token ou client
 * secret.
 */
public record CreateKeycloakUserResponse(String keycloakUserId) {
}
