package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.service.OtpService;

/**
 * Caso de uso para autenticação por telefone/OTP (Sprint 7.3).
 */
public interface PhoneAuthUseCase {

    /**
     * Solicita envio de OTP para o telefone.
     */
    PhoneAuthUseCase.SendOtpResult sendOtp(String phone);

    /**
     * Valida OTP e autentica usuário (se telefone já cadastrado)
     * ou retorna dados para cadastro (se telefone novo).
     */
    PhoneAuthUseCase.VerifyOtpResult verifyOtp(String phone, String otp);

    record SendOtpResult(boolean sent, String phoneE164, int ttlSeconds, int resendCooldownSeconds) {}

    record VerifyOtpResult(
            boolean success,
            String errorCode,
            boolean userExists,
            String userId,
            String email,
            boolean phoneVerified,
            String message
    ) {
        public static VerifyOtpResult success(String userId, String email, boolean phoneVerified) {
            return new VerifyOtpResult(true, null, true, userId, email, phoneVerified, "Autenticação realizada");
        }

        public static VerifyOtpResult userNotFound(String phoneE164) {
            return new VerifyOtpResult(false, "USER_NOT_FOUND", false, null, null, false, 
                    "Telefone não cadastrado. Complete o cadastro.");
        }

        public static VerifyOtpResult invalidOtp(String errorCode) {
            return new VerifyOtpResult(false, errorCode, false, null, null, false, "Código inválido");
        }
    }
}