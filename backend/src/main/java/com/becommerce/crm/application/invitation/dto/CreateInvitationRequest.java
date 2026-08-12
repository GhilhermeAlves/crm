package com.becommerce.crm.application.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Criação de convite (Sprint 8.5). Role validada no serviço (whitelist). */
public record CreateInvitationRequest(
        @NotBlank @Email String email,
        @NotBlank String role
) {}