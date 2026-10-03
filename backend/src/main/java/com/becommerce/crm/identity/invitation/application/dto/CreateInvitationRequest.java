package com.becommerce.crm.identity.invitation.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Criação de convite (Sprint 8.5). Role validada no serviço (whitelist). */
public record CreateInvitationRequest(
        @NotBlank @Email String email,
        @NotBlank String role
) {}