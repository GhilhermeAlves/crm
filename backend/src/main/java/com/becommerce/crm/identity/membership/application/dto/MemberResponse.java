package com.becommerce.crm.identity.membership.application.dto;

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
        LocalDateTime joinedAt,
        String specialty,
        /** Dono da plataforma (SUPER_ADMIN): tem todas as permissões em qualquer empresa. */
        Boolean platformAdmin,
        /** Tem ao menos um perfil RBAC nesta empresa (null quando não calculado). */
        Boolean hasProfile) {

    public MemberResponse(UUID userId, String name, String email, String role, String status,
                          LocalDateTime joinedAt, String specialty) {
        this(userId, name, email, role, status, joinedAt, specialty, null, null);
    }

    public MemberResponse(UUID userId, String name, String email, String role, String status,
                          LocalDateTime joinedAt) {
        this(userId, name, email, role, status, joinedAt, null, null, null);
    }
}
