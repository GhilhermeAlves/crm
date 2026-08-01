package com.becommerce.crm.infrastructure.security.filter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Contexto de negócio do usuário autenticado, resolvido a partir do JWT do
 * Keycloak e do banco CRM. Substitui o {@code CrmPrincipal} na Sprint 4 e é
 * compatível com o {@code CurrentUser} do crm-auth-service (mesmos campos).
 */
public record CurrentUser(
        UUID userId,
        String email,
        UUID companyId,
        UUID tenantId,
        List<String> roles,
        List<String> permissions,
        String keycloakSub,
        String sessionId,
        String provider,
        String displayName) {

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(keycloakSub, "keycloakSub");
        tenantId = tenantId == null ? companyId : tenantId;
        provider = provider == null || provider.isBlank() ? "keycloak" : provider;
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }

    public static CurrentUser fromKeycloak(UUID userId, String email, UUID companyId,
                                           List<String> roles, List<String> permissions,
                                           String keycloakSub, String displayName) {
        return new CurrentUser(userId, email, companyId, companyId, roles, permissions,
                keycloakSub, null, "keycloak", displayName);
    }
}
