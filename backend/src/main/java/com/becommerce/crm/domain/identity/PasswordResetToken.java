package com.becommerce.crm.domain.identity;

import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetToken {
    private UUID id;
    private String token;
    private UUID userId;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    public static PasswordResetToken create(String token, UUID userId, int expiryMinutes) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.id = UUID.randomUUID();
        resetToken.token = token;
        resetToken.userId = userId;
        resetToken.expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);
        resetToken.used = false;
        resetToken.createdAt = LocalDateTime.now();
        return resetToken;
    }

    public void markAsUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.used && !isExpired();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
