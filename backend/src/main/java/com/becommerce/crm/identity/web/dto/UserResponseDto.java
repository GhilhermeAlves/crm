package com.becommerce.crm.identity.web.dto;

public record UserResponseDto(
    String id,
    String email,
    String name,
    String companyId,
    boolean isActive,
    String createdAt,
    String updatedAt
) {
}
