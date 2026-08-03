package com.becommerce.auth.application.gateway.port.output;

/**
 * Porta de saída para a troca do authorization code por tokens (token endpoint
 * do Keycloak). A troca acontece exclusivamente no servidor — o
 * {@code codeVerifier} e o {@code clientSecret} nunca saem do Auth Service.
 */
public interface OidcTokenClient {

    TokenResponse exchange(ExchangeRequest request);

    record ExchangeRequest(String code, String codeVerifier) {
    }

    record TokenResponse(String accessToken, String refreshToken, String idToken,
                         long expiresInSeconds) {
    }
}
