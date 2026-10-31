package com.becommerce.crm.identity.invitation.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Convite do dono de uma empresa pré-cadastrada (CRM_ADMIN). O papel é sempre
 * OWNER; {@code sendEmail} é opcional e, quando {@code false}, o link fica
 * disponível apenas para cópia/WhatsApp.
 */
public record CreateOwnerInvitationRequest(
        @NotBlank @Email String email,
        @Size(max = 255) String name,
        Boolean sendEmail
) {
    public CreateOwnerInvitationRequest(String email, String name) {
        this(email, name, null);
    }

    public boolean shouldSendEmail() {
        return sendEmail == null || sendEmail;
    }
}
