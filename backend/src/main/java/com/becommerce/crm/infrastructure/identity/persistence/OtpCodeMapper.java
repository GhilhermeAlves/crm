package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.OtpCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OtpCodeMapper {

    public OtpCodeJpaEntity toJpaEntity(OtpCode otpCode) {
        OtpCodeJpaEntity entity = new OtpCodeJpaEntity();
        entity.setId(otpCode.getId());
        entity.setPhoneE164(otpCode.getPhoneE164());
        entity.setOtpHash(otpCode.getOtpHash());
        entity.setAttempts(otpCode.getAttempts());
        entity.setExpiresAt(otpCode.getExpiresAt());
        entity.setCreatedAt(otpCode.getCreatedAt());
        entity.setConsumedAt(otpCode.getConsumedAt());
        return entity;
    }

    public OtpCode toDomainEntity(OtpCodeJpaEntity entity) {
        OtpCode otpCode = new OtpCode();
        otpCode.setId(entity.getId());
        otpCode.setPhoneE164(entity.getPhoneE164());
        otpCode.setOtpHash(entity.getOtpHash());
        otpCode.setAttempts(entity.getAttempts());
        otpCode.setExpiresAt(entity.getExpiresAt());
        otpCode.setCreatedAt(entity.getCreatedAt());
        otpCode.setConsumedAt(entity.getConsumedAt());
        return otpCode;
    }
}