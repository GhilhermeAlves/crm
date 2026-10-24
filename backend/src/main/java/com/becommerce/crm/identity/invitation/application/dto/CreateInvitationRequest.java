package com.becommerce.crm.identity.invitation.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Criação de convite (Sprint 8.5). Role validada no serviço (whitelist).
 * {@code name} é opcional: aparece na lista de pendentes e pré-preenche o cadastro.
 * {@code specialty} opcional: especialidade do profissional convidado (ex.: TRICHOLOGY, DENTAL).
 */
public record CreateInvitationRequest(
        @NotBlank @Email String email,
        @NotBlank String role,
        @Size(max = 255) String name,
        String specialty
) {
    public CreateInvitationRequest(String email, String role) {
        this(email, role, null, null);
    }

    public CreateInvitationRequest(String email, String role, String name) {
        this(email, role, name, null);
    }
}
