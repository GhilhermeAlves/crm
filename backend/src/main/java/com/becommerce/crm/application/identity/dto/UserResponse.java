package com.becommerce.crm.application.identity.dto;

public record UserResponse(
    String id,
    String email,
    String name,
    String companyId,
    boolean isActive,
    String createdAt,
    String updatedAt
) {
}
