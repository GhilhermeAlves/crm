package com.becommerce.crm.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(
        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotBlank(message = "Senha é obrigatória")
        String password
) {
}
