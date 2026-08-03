package com.becommerce.auth.domain.gateway;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Sessão de browser do Access Gateway (Sprints 6.1/6.2). Criada <b>somente</b>
 * após a decisão positiva de CRM Access e referenciada por um cookie
 * HttpOnly/SameSite/Secure cujo valor é o {@code sessionToken} opaco —
 * <b>nunca</b> um JWT.
 *
 * <p>Os dados de autorização (roles/permissions) são espelhados do
 * {@code CurrentUser} resolvido no momento do login, permitindo o backend validar
 * autorização sem redescobrir o contexto.
 *
 * <p>Ciclo de vida (Sprint 6.2):
 * <ul>
 *   <li>TTL absoluto ({@code expiresAt}) + idle timeout ({@code lastAccessedAt}) —
 *       a expiração efetiva é {@code min(expiresAt, lastAccessedAt + idleTimeout)};</li>
 *   <li>{@code revokedAt} != null marca a sessão como revogada (tombstone);</li>
 *   <li>tokens (access/refresh/idToken) existem <b>somente no servidor</b> — nunca
 *       no browser; o {@code idTokenHint} é usado como {@code id_token_hint} no
 *       logout;</li>
 *   <li>{@code csrfToken} para o padrão cookie-to-header em {@code POST /auth/refresh}.</li>
 * </ul>
 *
 * <p>A sessão é serializável (apenas String/UUID/Instant/List — sem objetos
 * complexos), preparando a migração futura para Redis.
 */
public record GatewaySession(
        String sessionToken,
        UUID userId,
        String email,
        UUID companyId,
        UUID tenantId,
        List<String> roles,
        List<String> permissions,
        String keycloakSub,
        String keycloakSessionId,
        String provider,
        String displayName,
        Instant createdAt,
        Instant expiresAt,
        Instant lastAccessedAt,
        String idTokenHint,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        String csrfToken,
        Instant revokedAt) {

    public GatewaySession {
        Objects.requireNonNull(sessionToken, "sessionToken");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(keycloakSub, "keycloakSub");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(lastAccessedAt, "lastAccessedAt");
        Objects.requireNonNull(idTokenHint, "idTokenHint");
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt");
        Objects.requireNonNull(csrfToken, "csrfToken");
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }

    /**
     * Expiração efetiva = {@code min(TTL absoluto, lastAccessedAt + idleTimeout)}.
     * O idle timeout é desabilitado quando {@code null}, zero ou negativo.
     */
    public Instant effectiveExpiration(Duration idleTimeout) {
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()) {
            return expiresAt;
        }
        Instant idleExpiration = lastAccessedAt.plus(idleTimeout);
        return idleExpiration.isBefore(expiresAt) ? idleExpiration : expiresAt;
    }

    public boolean isActive(Instant now, Duration idleTimeout) {
        return revokedAt == null && now.isBefore(effectiveExpiration(idleTimeout));
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public GatewaySession withLastAccessed(Instant now) {
        return new GatewaySession(sessionToken, userId, email, companyId, tenantId, roles, permissions,
                keycloakSub, keycloakSessionId, provider, displayName, createdAt, expiresAt, now,
                idTokenHint, accessToken, refreshToken, accessTokenExpiresAt, csrfToken, revokedAt);
    }

    public GatewaySession withRevokedAt(Instant now) {
        return new GatewaySession(sessionToken, userId, email, companyId, tenantId, roles, permissions,
                keycloakSub, keycloakSessionId, provider, displayName, createdAt, expiresAt, lastAccessedAt,
                idTokenHint, accessToken, refreshToken, accessTokenExpiresAt, csrfToken, now);
    }

    public GatewaySession withRotatedTokens(String newAccessToken, String newRefreshToken, String newIdTokenHint,
                                            Instant newAccessTokenExpiresAt, Instant now) {
        String hint = (newIdTokenHint == null || newIdTokenHint.isBlank()) ? idTokenHint : newIdTokenHint;
        return new GatewaySession(sessionToken, userId, email, companyId, tenantId, roles, permissions,
                keycloakSub, keycloakSessionId, provider, displayName, createdAt, expiresAt, now,
                hint, newAccessToken, newRefreshToken, newAccessTokenExpiresAt, csrfToken, revokedAt);
    }
}
