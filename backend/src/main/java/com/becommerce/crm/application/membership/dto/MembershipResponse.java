package com.becommerce.crm.application.membership.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Membership do usuário autenticado (para {@code GET /api/v1/me/memberships}).
 */
public record MembershipResponse(
        UUID companyId,
        String companyName,
        String role,
        String status,
        LocalDateTime joinedAt) {
}
