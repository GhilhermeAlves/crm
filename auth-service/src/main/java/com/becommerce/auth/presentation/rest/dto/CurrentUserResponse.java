package com.becommerce.auth.presentation.rest.dto;

import com.becommerce.auth.domain.identity.CurrentUser;

import java.util.List;
import java.util.UUID;

/**
 * Representação JSON (camelCase) do CurrentUser, compatível com CURRENT_USER.md.
 */
public record CurrentUserResponse(
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

    public static CurrentUserResponse from(CurrentUser currentUser) {
        return new CurrentUserResponse(
                currentUser.userId(),
                currentUser.email(),
                currentUser.companyId(),
                currentUser.tenantId(),
                currentUser.roles(),
                currentUser.permissions(),
                currentUser.keycloakSub(),
                currentUser.sessionId(),
                currentUser.provider(),
                currentUser.displayName());
    }
}
