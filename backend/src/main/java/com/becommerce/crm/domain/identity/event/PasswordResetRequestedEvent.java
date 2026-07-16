package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordResetRequestedEvent(
    UUID userId,
    UUID companyId,
    LocalDateTime occurredAt
) {
    public static PasswordResetRequestedEvent create(UUID userId, UUID companyId) {
        return new PasswordResetRequestedEvent(userId, companyId, LocalDateTime.now());
    }
}
