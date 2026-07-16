package com.becommerce.crm.application.identity.dto;

import java.time.LocalDateTime;

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
        LocalDateTime updatedAt
) {
    public UserResponse {
        if (name == null && firstName != null && lastName != null) {
            name = firstName + " " + lastName;
        }
    }
}
