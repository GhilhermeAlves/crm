package com.becommerce.crm.domain.identity;

import java.time.LocalDateTime;
import java.util.UUID;

public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private String family;
    private LocalDateTime expiresAt;
    private boolean isRevoked;
    private LocalDateTime createdAt;

    public RefreshToken() {
    }

    public static RefreshToken create(UUID userId, String token, String family, int expiryDays) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.id = UUID.randomUUID();
        refreshToken.userId = userId;
        refreshToken.token = token;
        refreshToken.family = family;
        refreshToken.expiresAt = LocalDateTime.now().plusDays(expiryDays);
        refreshToken.isRevoked = false;
        refreshToken.createdAt = LocalDateTime.now();
        return refreshToken;
    }

    public void revoke() {
        this.isRevoked = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.isRevoked && !isExpired();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean revoked) {
        isRevoked = revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
