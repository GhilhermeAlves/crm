package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserLoggedInEvent(
    UUID userId,
    UUID companyId,
    String ipAddress,
    LocalDateTime occurredAt
) {
    public static UserLoggedInEvent create(UUID userId, UUID companyId, String ipAddress) {
        return new UserLoggedInEvent(userId, companyId, ipAddress, LocalDateTime.now());
    }
}
