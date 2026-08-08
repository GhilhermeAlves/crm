package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.port.input.PhoneAuthUseCase;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.domain.identity.OtpCode;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.valueobject.PhoneNumber;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do caso de uso de autenticação por telefone (Sprint 7.3).
 */
@Service
public class PhoneAuthService implements PhoneAuthUseCase {

    private final OtpService otpService;
    private final UserRepository userRepository;

    public PhoneAuthService(OtpService otpService, UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @Override
    public SendOtpResult sendOtp(String phone) {
        try {
            PhoneNumber phoneNumber = new PhoneNumber(phone);
            String phoneE164 = phoneNumber.getE164();

            // Verifica cooldown de reenvio
            if (!otpService.canResend(phoneE164)) {
                return new SendOtpResult(false, phoneE164, 0, otpService.getResendCooldownSeconds());
            }

            OtpService.OtpGenerationResult result = otpService.generateOtp(phoneE164);
            return new SendOtpResult(true, result.phoneE164(), result.ttlSeconds(), otpService.getResendCooldownSeconds());
        } catch (IllegalArgumentException e) {
            return new SendOtpResult(false, null, 0, 0);
        }
    }

    @Override
    public VerifyOtpResult verifyOtp(String phone, String otp) {
        try {
            PhoneNumber phoneNumber = new PhoneNumber(phone);
            String phoneE164 = phoneNumber.getE164();

            OtpService.OtpValidationResult validation = otpService.validateOtp(phoneE164, otp);
            if (!validation.isSuccess()) {
                return VerifyOtpResult.invalidOtp(validation.getErrorCode());
            }

            // OTP válido comprova a POSSE do telefone — habilita a leitura da
            // própria linha em users via app.current_identity_phone (RLS FORCE).
            TenantContext.setIdentityPhone(phoneE164);
            try {
                // Busca usuário pelo telefone
                Optional<User> userOpt = userRepository.findByPhone(phoneE164);
                if (userOpt.isEmpty()) {
                    return VerifyOtpResult.userNotFound(phoneE164);
                }

                User user = userOpt.get();

                // Marca telefone como verificado se não estiver
                if (!user.isPhoneVerified()) {
                    user.markPhoneVerified();
                    userRepository.save(user);
                }

                return VerifyOtpResult.success(user.getId().toString(), user.getEmail().value(), user.isPhoneVerified());
            } finally {
                TenantContext.clearIdentityPhone();
            }
        } catch (IllegalArgumentException e) {
            return VerifyOtpResult.invalidOtp("INVALID_PHONE");
        }
    }
}