package com.becommerce.crm.clinical.evolution.application.dto;

import com.becommerce.crm.clinical.evolution.domain.PatientEvolutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientEvolutionResponse(
        UUID id,
        UUID contactId,
        UUID appointmentId,
        String body,
        PatientEvolutionStatus status,
        UUID createdBy,
        String createdByName,
        UUID finalizedBy,
        String finalizedByName,
        LocalDateTime finalizedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
