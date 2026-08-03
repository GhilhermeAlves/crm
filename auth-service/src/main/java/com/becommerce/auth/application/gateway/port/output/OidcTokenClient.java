package com.becommerce.auth.application.gateway.port.output;

/**
 * Porta de saída para o token endpoint do Keycloak (Sprints 6.1/6.2): troca do
 * authorization code por tokens e renovação (refresh) com rotação. As chamadas
 * acontecem exclusivamente no servidor — o {@code codeVerifier}, o
 * {@code clientSecret} e os refresh tokens nunca saem do Auth Service nem são
 * devolvidos ao browser.
 */
public interface OidcTokenClient {

    TokenResponse exchange(ExchangeRequest request);

    TokenResponse refresh(RefreshRequest request);

    record ExchangeRequest(String code, String codeVerifier) {
    }

    record RefreshRequest(String refreshToken) {
    }

    record TokenResponse(String accessToken, String refreshToken, String idToken,
                         long expiresInSeconds) {
    }
}
