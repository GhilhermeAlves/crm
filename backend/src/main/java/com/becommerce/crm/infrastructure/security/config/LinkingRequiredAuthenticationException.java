package com.becommerce.crm.infrastructure.security.config;

import org.springframework.security.core.AuthenticationException;

/**
 * Autenticação externa (ex.: Google) cujo e-mail corresponde a uma conta local
 * existente, porém sem {@code keycloak_sub} vinculado (Sprint 7.2). Permite ao
 * {@link JwtAuthenticationEntryPoint} responder {@code 409 LINKING_REQUIRED}
 * (defense in depth — no fluxo gateway isso nunca chega ao backend, pois o
 * linking é resolvido antes de criar a sessão).
 */
public class LinkingRequiredAuthenticationException extends AuthenticationException {

    public LinkingRequiredAuthenticationException(String message) {
        super(message);
    }
}
