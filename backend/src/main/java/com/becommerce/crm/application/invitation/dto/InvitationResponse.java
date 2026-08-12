package com.becommerce.crm.application.invitation.dto;

import com.becommerce.crm.domain.invitation.InvitationStatus;

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