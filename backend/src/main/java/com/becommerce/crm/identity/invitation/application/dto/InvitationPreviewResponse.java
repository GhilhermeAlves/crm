package com.becommerce.crm.identity.invitation.application.dto;

import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

import java.time.LocalDateTime;

/**
 * Prévia pública do convite (sem login), exibida em /convite/{token}.
 * {@code status} é o efetivo: PENDING vencido aparece como EXPIRED.
 * {@code hasAccount} decide entre "entrar para aceitar" e "criar conta".
 */
public record InvitationPreviewResponse(
        String companyName,
        String email,
        String inviteeName,
        String role,
        InvitationStatus status,
        LocalDateTime expiresAt,
        boolean hasAccount
) {}
