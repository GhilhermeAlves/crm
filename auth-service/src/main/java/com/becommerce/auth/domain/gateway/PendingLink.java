package com.becommerce.auth.domain.gateway;

import java.time.Instant;
import java.util.Objects;

/**
 * Vínculo pendente de uma identidade de provedor externo (Sprint 7.2, Caso B):
 * o e-mail coincide com uma conta local, porém sem {@code keycloak_sub}. O
 * usuário verifica a senha da conta local em {@code /link-account} antes do
 * vínculo efetivo no crm-backend.
 *
 * <p>Criado durante o {@code /auth/callback} e referenciado por um cookie
 * HttpOnly curto ({@code crm_pending_link}). Retém os tokens (somente no
 * servidor) para completar a sessão de browser após o vínculo — o browser nunca
 * os vê. Uso único: consumido ao vincular com sucesso; TTL curto (10 min).
 */
public record PendingLink(
        String token,
        String keycloakSub,
        String email,
        String displayName,
        String provider,
        String csrfToken,
        String idToken,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        String redirectTarget,
        Instant createdAt,
        Instant expiresAt) {

    public PendingLink {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(keycloakSub, "keycloakSub");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(csrfToken, "csrfToken");
        Objects.requireNonNull(idToken, "idToken");
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(redirectTarget, "redirectTarget");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
