package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordChangedEvent(
    UUID userId,
    UUID companyId,
    LocalDateTime occurredAt
) {
    public static PasswordChangedEvent create(UUID userId, UUID companyId) {
        return new PasswordChangedEvent(userId, companyId, LocalDateTime.now());
    }
}
