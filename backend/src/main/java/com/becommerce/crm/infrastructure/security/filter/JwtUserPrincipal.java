package com.becommerce.crm.infrastructure.security.filter;

import java.util.UUID;

public record JwtUserPrincipal(UUID userId, UUID companyId) {
}
