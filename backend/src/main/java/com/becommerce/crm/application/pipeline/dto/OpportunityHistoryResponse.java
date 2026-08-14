package com.becommerce.crm.application.pipeline.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpportunityHistoryResponse(
        UUID id,
        UUID opportunityId,
        UUID fromStageId,
        UUID toStageId,
        UUID changedBy,
        LocalDateTime changedAt,
        String note
) {}
