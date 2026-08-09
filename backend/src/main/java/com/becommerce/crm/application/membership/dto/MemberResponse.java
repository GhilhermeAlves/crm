package com.becommerce.crm.application.membership.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Membro ativo de uma empresa (para {@code GET /api/v1/companies/{id}/members}).
 */
public record MemberResponse(
        UUID userId,
        String name,
        String email,
        String role,
        String status,
        LocalDateTime joinedAt) {
}
