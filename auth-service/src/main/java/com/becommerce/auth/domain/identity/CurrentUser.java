package com.becommerce.auth.domain.identity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Contexto de negócio do usuário autenticado, resolvido pelo crm-auth-service a
 * partir do JWT oficial do Keycloak e do banco CRM. Imutável e distribuído como
 * payload (não é um token, não é um novo JWT).
 *
 * <p>Compatível com o {@code CrmPrincipal} do crm-backend: {@code userId},
 * {@code companyId}, {@code roles}, {@code permissions}, {@code keycloakSub}
 * mantêm a mesma semântica; novos campos documentados em CURRENT_USER.md
 * ({@code email}, {@code tenantId}, {@code sessionId}, {@code provider},
 * {@code displayName}).
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

    public CurrentUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(keycloakSub, "keycloakSub");
        // Sprint 8.3: companyId pode ser null (usuário sem empresa — onboarding
        // pendente). Nesse estado o CurrentUser é autenticado, porém SEM empresa
        // e SEM roles/permissions.
        tenantId = tenantId == null ? companyId : tenantId;
        provider = provider == null || provider.isBlank() ? "keycloak" : provider;
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }
}
