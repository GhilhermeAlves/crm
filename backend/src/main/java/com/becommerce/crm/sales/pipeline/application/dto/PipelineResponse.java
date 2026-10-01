package com.becommerce.crm.sales.pipeline.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PipelineResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        boolean active,
        List<StageResponse> stages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
