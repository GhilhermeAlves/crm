package com.becommerce.crm.application.identity.dto;

public record UpdateProfileRequest(
        String firstName,
        String lastName,
        String phone,
        String department,
        String jobTitle,
        String language,
        String timezone,
        String notes,
        String avatarUrl
) {
}
