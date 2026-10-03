package com.becommerce.crm.communication.notification.application.dto;

import com.becommerce.crm.communication.notification.domain.NotificationType;

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