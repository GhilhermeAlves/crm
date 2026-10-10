package com.becommerce.crm.masterdata.anamnesis.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Modelo completo com árvore de seções/perguntas/opções. */
public record AnamnesisModelResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        boolean active,
        boolean isDefault,
        int version,
        List<AnamnesisSectionResponse> sections,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
