package com.becommerce.crm.domain.identity.repository;

import com.becommerce.crm.domain.identity.OtpCode;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para códigos OTP (Sprint 7.3).
 */
public interface OtpCodeRepository {
    OtpCode save(OtpCode otpCode);
    Optional<OtpCode> findLatestByPhone(String phoneE164);
    Optional<OtpCode> findById(UUID id);
    void deleteExpired();
}