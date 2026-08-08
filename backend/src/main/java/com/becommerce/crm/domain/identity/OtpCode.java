package com.becommerce.crm.domain.identity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Código OTP temporário para verificação de telefone (Sprint 7.3).
 * Armazenado com hash, expiração e contador de tentativas.
 */
public class OtpCode {
    private UUID id;
    private String phoneE164;
    private String otpHash;
    private int attempts;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime consumedAt;

    public OtpCode() {
    }

    public static OtpCode create(String phoneE164, String otpHash, int ttlMinutes, int maxAttempts) {
        OtpCode code = new OtpCode();
        code.phoneE164 = phoneE164;
        code.otpHash = otpHash;
        code.attempts = 0;
        code.expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
        code.createdAt = LocalDateTime.now();
        return code;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean canAttempt(int maxAttempts) {
        return attempts < maxAttempts && !isExpired() && !isConsumed();
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public void markConsumed() {
        this.consumedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}