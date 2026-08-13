package com.becommerce.crm.application.identity.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String name,
        String phone,
        String department,
        String jobTitle,
        String avatarUrl,
        String companyId,
        String status,
        boolean isActive,
        String language,
        String timezone,
        String notes,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> roles,
        String membershipRole,
        List<String> permissions
) {
    public UserResponse {
        if (name == null && firstName != null && lastName != null) {
            name = firstName + " " + lastName;
        }
        if (roles == null) {
            roles = List.of();
        }
        if (permissions == null) {
            permissions = List.of();
        }
    }

    /** Fábrica para {@code /auth/me}: acopla identidade + contexto efetivo da empresa ativa. */
    public static UserResponse withCurrentUser(UserResponse base, List<String> roles,
                                               String membershipRole, List<String> permissions) {
        return new UserResponse(
                base.id(), base.email(), base.firstName(), base.lastName(), base.name(),
                base.phone(), base.department(), base.jobTitle(), base.avatarUrl(),
                base.companyId(), base.status(), base.isActive(), base.language(),
                base.timezone(), base.notes(), base.lastLoginAt(), base.createdAt(),
                base.updatedAt(), roles, membershipRole, permissions);
    }
}
