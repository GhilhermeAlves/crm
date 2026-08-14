package com.becommerce.crm.application.activity.dto;

import com.becommerce.crm.domain.activity.ActivityType;

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