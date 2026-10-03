package com.becommerce.crm.identity.application.port.output;

import com.becommerce.crm.identity.domain.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken passwordResetToken);
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(UUID userId);
}
