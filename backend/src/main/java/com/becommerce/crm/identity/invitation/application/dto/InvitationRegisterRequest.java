package com.becommerce.crm.identity.invitation.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro por convite. Não há campo de e-mail: o e-mail é sempre o do convite,
 * resolvido no servidor a partir do token.
 */
public record InvitationRegisterRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 255) String name,
        @NotBlank String password
) {
    @Override
    public String toString() {
        return "InvitationRegisterRequest[name=" + name + "]";
    }
}
