package com.becommerce.crm.application.identity.dto;

public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
