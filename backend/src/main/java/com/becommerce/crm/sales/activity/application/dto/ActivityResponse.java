package com.becommerce.crm.sales.activity.application.dto;

import com.becommerce.crm.sales.activity.domain.ActivityType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID companyId,
        UUID contactId,
        UUID opportunityId,
        ActivityType type,
        String subject,
        String description,
        LocalDateTime activityAt,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}