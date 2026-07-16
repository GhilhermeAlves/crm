package com.becommerce.crm.application.identity.port.output;

import com.becommerce.crm.domain.identity.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserIdAndFamily(UUID userId, String family);
    void revokeAllByUserId(UUID userId);
    void revokeByToken(String token);
    void deleteExpiredTokens();
}
