package com.becommerce.crm.application.customer360.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resumo do contato para o Customer 360. */
public record ContactSummaryResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String notes,
        String initials,
        LocalDateTime createdAt,
        LocalDateTime lastInteractionAt,
        boolean atRisk,
        String riskMessage
) {}