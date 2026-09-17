package com.becommerce.crm.application.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email) {
}