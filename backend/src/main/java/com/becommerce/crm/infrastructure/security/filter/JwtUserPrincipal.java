package com.becommerce.crm.infrastructure.security.filter;

import java.util.List;
import java.util.UUID;

public record JwtUserPrincipal(UUID userId, UUID companyId, List<String> roles, List<String> permissions) {
}
