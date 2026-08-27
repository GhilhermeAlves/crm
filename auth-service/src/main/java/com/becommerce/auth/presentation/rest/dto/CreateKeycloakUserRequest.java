package com.becommerce.auth.presentation.rest.dto;

/**
 * Contrato de entrada do {@code POST /internal/auth/create-user} (Sprint 8.5).
 * Chamado pelo crm-backend de serviço a serviço (header
 * {@code X-Internal-Api-Token}). A senha é texto puro — via corpo HTTP somente,
 * nunca logada.
 */
public record CreateKeycloakUserRequest(
        String email,
        String password,
        String name) {
}
