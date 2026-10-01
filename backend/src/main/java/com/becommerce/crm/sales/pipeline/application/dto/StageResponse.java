package com.becommerce.crm.sales.pipeline.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StageResponse(
        UUID id,
        UUID pipelineId,
        String name,
        String color,
        int order,
        int probability,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
