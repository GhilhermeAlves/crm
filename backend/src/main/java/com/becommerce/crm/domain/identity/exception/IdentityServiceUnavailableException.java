package com.becommerce.crm.domain.identity.exception;

/**
 * Serviço de identidade (auth-service/Keycloak) indisponível ou em falha de
 * comunicação. Mapeado para 503 Service Unavailable no GlobalExceptionHandler —
 * o cadastro não deve mascarar a indisponibilidade como erro genérico.
 */
public class IdentityServiceUnavailableException extends RuntimeException {

    public IdentityServiceUnavailableException(String message) {
        super(message);
    }
}