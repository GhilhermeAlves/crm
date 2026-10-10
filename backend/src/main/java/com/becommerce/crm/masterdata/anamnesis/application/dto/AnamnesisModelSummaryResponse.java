package com.becommerce.crm.masterdata.anamnesis.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resumo para a listagem (sem estrutura de perguntas). */
public record AnamnesisModelSummaryResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        boolean active,
        boolean isDefault,
        int version,
        int sectionCount,
        int questionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
