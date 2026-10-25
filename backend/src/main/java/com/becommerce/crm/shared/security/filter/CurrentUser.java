package com.becommerce.crm.shared.security.filter;

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
        String displayName,
        String membershipRole) {

    /**
     * Papel de plataforma, acima do SUPER_ADMIN: único que enxerga e administra
     * todas as empresas. Não é criado por empresa nem concedível pela API —
     * atribuído só por migration.
     */
    public static final String CRM_ADMIN_ROLE = "CRM_ADMIN";

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(keycloakSub, "keycloakSub");
        // Sprint 8.3: companyId pode ser null (usuário sem empresa — onboarding
        // pendente). Nesse estado o CurrentUser é autenticado, porém SEM empresa
        // e SEM roles/permissions (gate: apenas a tela de onboarding).
        tenantId = tenantId == null ? companyId : tenantId;
        provider = provider == null || provider.isBlank() ? "keycloak" : provider;
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }

    /** True se o usuário pode cruzar empresas (papel {@link #CRM_ADMIN_ROLE}). */
    public boolean isCrmAdmin() {
        return roles.contains(CRM_ADMIN_ROLE);
    }

    public static CurrentUser fromKeycloak(UUID userId, String email, UUID companyId,
                                           List<String> roles, List<String> permissions,
                                           String keycloakSub, String displayName) {
        return fromKeycloak(userId, email, companyId, roles, permissions, keycloakSub, displayName, null);
    }

    public static CurrentUser fromKeycloak(UUID userId, String email, UUID companyId,
                                           List<String> roles, List<String> permissions,
                                           String keycloakSub, String displayName, String membershipRole) {
        return new CurrentUser(userId, email, companyId, companyId, roles, permissions,
                keycloakSub, null, "keycloak", displayName, membershipRole);
    }
}
