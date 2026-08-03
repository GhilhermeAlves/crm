package com.becommerce.auth.domain.gateway;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Sessão de browser do Access Gateway (Sprint 6.1). Criada <b>somente</b> após a
 * decisão positiva de CRM Access (usuário + empresa ativos) e referenciada por um
 * cookie HttpOnly/SameSite/Secure cujo valor é o {@code sessionToken} opaco —
 * <b>nunca</b> um JWT.
 *
 * <p>Os dados de autorização (roles/permissions) são espelhados do
 * {@code CurrentUser} resolvido no momento do login, permitindo o backend validar
 * autorização sem redescobrir o contexto.
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
        Instant expiresAt) {

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
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
