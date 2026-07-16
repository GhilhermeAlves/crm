package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserLoggedOutEvent(
    UUID userId,
    UUID companyId,
    LocalDateTime occurredAt
) {
    public static UserLoggedOutEvent create(UUID userId, UUID companyId) {
        return new UserLoggedOutEvent(userId, companyId, LocalDateTime.now());
    }
}
