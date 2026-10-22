package com.becommerce.crm.clinical.anamnesis.application.dto;

import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Anamnese preenchida com o snapshot do modelo, respostas e alertas. */
public record PatientAnamnesisResponse(
        UUID id,
        UUID contactId,
        UUID modelId,
        String modelName,
        int modelVersion,
        AnamnesisModelResponse snapshot,
        Map<UUID, PatientAnamnesisAnswer> answers,
        List<String> alerts,
        PatientAnamnesisStatus status,
        UUID createdBy,
        String createdByName,
        UUID completedBy,
        String completedByName,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
