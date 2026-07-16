package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken passwordResetToken);
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(UUID userId);
}
