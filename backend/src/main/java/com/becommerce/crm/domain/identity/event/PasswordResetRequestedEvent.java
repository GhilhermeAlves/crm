package com.becommerce.crm.domain.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordResetRequestedEvent(
    UUID userId,
    UUID companyId,
    String email,
    String resetToken,
    LocalDateTime occurredAt
) {
    public static PasswordResetRequestedEvent create(UUID userId, UUID companyId, String email, String resetToken) {
        return new PasswordResetRequestedEvent(userId, companyId, email, resetToken, LocalDateTime.now());
    }
}
