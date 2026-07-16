package com.becommerce.crm.application.identity.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String userId,
    String email,
    String name
) {
}
