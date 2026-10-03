package com.becommerce.crm.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteUserRequest(
        @NotBlank(message = "Nome é obrigatório")
        String firstName,

        @NotBlank(message = "Sobrenome é obrigatório")
        String lastName,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        String department,

        String jobTitle
) {
}
