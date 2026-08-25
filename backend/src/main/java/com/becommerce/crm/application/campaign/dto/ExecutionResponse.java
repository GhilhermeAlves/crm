package com.becommerce.crm.application.campaign.dto;

import com.becommerce.crm.domain.campaign.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        UUID campaignId,
        ExecutionStatus status,
        int totalRecipients,
        int processedCount,
        int failedCount,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
