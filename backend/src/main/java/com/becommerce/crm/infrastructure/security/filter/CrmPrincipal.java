package com.becommerce.crm.infrastructure.security.filter;

import java.util.List;
import java.util.UUID;

public record CrmPrincipal(
    UUID userId,
    UUID companyId,
    List<String> roles,
    List<String> permissions,
    String keycloakSub
) {
    public static CrmPrincipal fromLegacy(UUID userId, UUID companyId, List<String> roles, List<String> permissions) {
        return new CrmPrincipal(userId, companyId, roles, permissions, null);
    }

    public static CrmPrincipal fromKeycloak(UUID userId, UUID companyId, String keycloakSub,
                                            List<String> roles, List<String> permissions) {
        return new CrmPrincipal(userId, companyId, roles, permissions, keycloakSub);
    }
}
