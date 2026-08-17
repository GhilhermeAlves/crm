package com.becommerce.crm.application.notification.dto;

import com.becommerce.crm.domain.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID companyId,
        UUID userId,
        NotificationType type,
        String title,
        String body,
        String metadata,
        LocalDateTime readAt,
        boolean read,
        LocalDateTime createdAt
) {
}