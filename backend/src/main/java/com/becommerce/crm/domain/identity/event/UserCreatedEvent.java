package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(
    UUID userId,
    String email,
    UUID companyId,
    LocalDateTime occurredAt
) {
    public static UserCreatedEvent create(UUID userId, String email, UUID companyId) {
        return new UserCreatedEvent(userId, email, companyId, LocalDateTime.now());
    }
}
