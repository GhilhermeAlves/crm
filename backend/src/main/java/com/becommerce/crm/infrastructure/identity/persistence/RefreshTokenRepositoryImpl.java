package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.RefreshTokenRepository;
import com.becommerce.crm.domain.identity.RefreshToken;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository repository;

    public RefreshTokenRepositoryImpl(SpringDataRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setUserId(refreshToken.getUserId());
        entity.setToken(refreshToken.getToken());
        entity.setFamily(refreshToken.getFamily());
        entity.setExpiresAt(refreshToken.getExpiresAt());
        entity.setRevoked(refreshToken.isRevoked());
        entity.setCreatedAt(refreshToken.getCreatedAt());

        RefreshTokenJpaEntity saved = repository.save(entity);

        long expiryDays = Duration.between(LocalDateTime.now(), saved.getExpiresAt()).toDays();
        if (expiryDays < 1) expiryDays = 1;
        return RefreshToken.create(saved.getUserId(), saved.getToken(), saved.getFamily(), (int) expiryDays);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token).map(entity -> {
            long expiryDays = Duration.between(LocalDateTime.now(), entity.getExpiresAt()).toDays();
            if (expiryDays < 1) expiryDays = 1;
            return RefreshToken.create(entity.getUserId(), entity.getToken(), entity.getFamily(), (int) expiryDays);
        });
    }

    @Override
    public Optional<RefreshToken> findByUserIdAndFamily(UUID userId, String family) {
        return repository.findByUserIdAndFamily(userId, family).map(entity -> {
            long expiryDays = Duration.between(LocalDateTime.now(), entity.getExpiresAt()).toDays();
            if (expiryDays < 1) expiryDays = 1;
            return RefreshToken.create(entity.getUserId(), entity.getToken(), entity.getFamily(), (int) expiryDays);
        });
    }

    @Override
    public void revokeAllByUserId(UUID userId) {
        repository.deleteByUserId(userId);
    }

    @Override
    public void revokeByToken(String token) {
        repository.findByToken(token).ifPresent(entity -> {
            entity.setRevoked(true);
            repository.save(entity);
        });
    }

    @Override
    public void deleteExpiredTokens() {
        repository.findAll().stream()
                .filter(entity -> entity.getExpiresAt().isBefore(LocalDateTime.now()))
                .forEach(repository::delete);
    }
}
