package com.becommerce.crm.identity.invitation.application.dto;

import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID companyId,
        String email,
        String role,
        InvitationStatus status,
        UUID invitedBy,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}