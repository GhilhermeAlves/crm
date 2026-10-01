package com.becommerce.crm.infrastructure.identity.client.dto;


/**
 * Corpo do {@code POST /internal/auth/create-user} (Sprint 8.5) chamado no
 * crm-auth-service. A senha é texto puro — via corpo HTTP somente, nunca logada.
 */
public record CreateKeycloakUserRequest(
        String email,
        String password,
        String name) {
}
