package com.becommerce.crm.application.identity.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
