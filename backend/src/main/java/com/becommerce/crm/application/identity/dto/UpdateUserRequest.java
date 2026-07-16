package com.becommerce.crm.application.identity.dto;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String department,
        String jobTitle,
        String avatarUrl,
        String language,
        String timezone,
        String notes
) {
}
