package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.domain.identity.PasswordResetToken;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository repository;

    public PasswordResetTokenRepositoryImpl(SpringDataPasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.setToken(passwordResetToken.getToken());
        entity.setUserId(passwordResetToken.getUserId());
        entity.setExpiresAt(passwordResetToken.getExpiresAt());
        entity.setUsed(passwordResetToken.isUsed());
        entity.setCreatedAt(passwordResetToken.getCreatedAt());

        PasswordResetTokenJpaEntity saved = repository.save(entity);

        long expiryMinutes = Duration.between(LocalDateTime.now(), saved.getExpiresAt()).toMinutes();
        if (expiryMinutes < 1) expiryMinutes = 1;
        return PasswordResetToken.create(saved.getToken(), saved.getUserId(), (int) expiryMinutes);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return repository.findByToken(token).map(entity -> {
            long expiryMinutes = Duration.between(LocalDateTime.now(), entity.getExpiresAt()).toMinutes();
            if (expiryMinutes < 1) expiryMinutes = 1;
            PasswordResetToken domainToken = PasswordResetToken.create(entity.getToken(), entity.getUserId(), (int) expiryMinutes);
            domainToken.setId(entity.getId());
            domainToken.setUsed(entity.isUsed());
            domainToken.setCreatedAt(entity.getCreatedAt());
            return domainToken;
        });
    }

    @Override
    public void deleteByUserId(UUID userId) {
        repository.deleteByUserId(userId);
    }
}
