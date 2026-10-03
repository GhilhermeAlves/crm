package com.becommerce.crm.sales.pipeline.application.dto;

import com.becommerce.crm.sales.pipeline.domain.OpportunityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OpportunityResponse(
        UUID id,
        UUID companyId,
        String title,
        BigDecimal value,
        UUID contactId,
        UUID pipelineId,
        UUID stageId,
        String stageName,
        int probability,
        UUID assignedTo,
        LocalDateTime expectedCloseDate,
        OpportunityStatus status,
        LocalDateTime wonAt,
        LocalDateTime lostAt,
        String lossReason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
