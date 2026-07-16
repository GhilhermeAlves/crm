package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record TokenRefreshedEvent(
    UUID userId,
    UUID companyId,
    LocalDateTime occurredAt
) {
    public static TokenRefreshedEvent create(UUID userId, UUID companyId) {
        return new TokenRefreshedEvent(userId, companyId, LocalDateTime.now());
    }
}
