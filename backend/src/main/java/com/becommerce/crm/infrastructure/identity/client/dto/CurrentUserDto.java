package com.becommerce.crm.infrastructure.identity.client.dto;

import com.becommerce.crm.infrastructure.security.filter.CurrentUser;

import java.util.List;
import java.util.UUID;

/**
 * Representação JSON (camelCase) do CurrentUser retornada pelo
 * {@code GET /internal/auth/current-user} do crm-auth-service.
 */
public record CurrentUserDto(
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

    public CurrentUser toCurrentUser() {
        return new CurrentUser(userId, email, companyId, tenantId, roles, permissions,
                keycloakSub, sessionId, provider, displayName);
    }
}
