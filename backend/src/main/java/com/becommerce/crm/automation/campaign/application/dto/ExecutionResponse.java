package com.becommerce.crm.automation.campaign.application.dto;

import com.becommerce.crm.automation.campaign.domain.ExecutionStatus;

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
