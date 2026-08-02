package com.becommerce.crm.infrastructure.security.config;

import org.springframework.security.core.AuthenticationException;

/**
 * Autenticação do Keycloak válida, mas acesso ao CRM negado (Sprint 6).
 * Permite ao {@link JwtAuthenticationEntryPoint} responder
 * {@code 403 CRM_ACCESS_DENIED} em vez do 401 genérico de autenticação.
 */
public class CrmAccessDeniedAuthenticationException extends AuthenticationException {

    public CrmAccessDeniedAuthenticationException(String message) {
        super(message);
    }
}
