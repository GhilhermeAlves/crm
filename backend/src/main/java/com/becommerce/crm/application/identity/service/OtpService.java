package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.port.output.OtpSender;
import com.becommerce.crm.domain.identity.OtpCode;
import com.becommerce.crm.domain.identity.repository.OtpCodeRepository;
import com.becommerce.crm.domain.identity.valueobject.PhoneNumber;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para geração e validação de OTP via telefone (Sprint 7.3).
 * 
 * Segurança:
 * - OTP armazenado apenas como hash (SHA-256 + salt)
 * - Expiração configurável (TTL)
 * - Limite de tentativas
 * - Invalidação após uso ou expiração
 * - Rate limiting por telefone
 */
@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int DEFAULT_TTL_MINUTES = 5;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final OtpCodeRepository otpCodeRepository;
    private final OtpSender otpSender;
    private final SecureRandom secureRandom;

    public OtpService(OtpCodeRepository otpCodeRepository, OtpSender otpSender) {
        this.otpCodeRepository = otpCodeRepository;
        this.otpSender = otpSender;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Gera e persiste um novo OTP para o telefone.
     * Invalida OTPs anteriores do mesmo telefone.
     */
    public OtpGenerationResult generateOtp(String phoneE164) {
        // Normaliza telefone
        PhoneNumber phone = new PhoneNumber(phoneE164);
        String normalized = phone.getE164();

        // Invalida OTPs anteriores não consumidos
        otpCodeRepository.findLatestByPhone(normalized)
                .filter(otp -> !otp.isConsumed() && !otp.isExpired())
                .ifPresent(otp -> otp.markConsumed());

        // Gera OTP numérico de 6 dígitos
        String otp = generateNumericOtp();
        
        // Hash do OTP com salt
        String salt = generateSalt();
        String otpHash = hashOtp(otp, salt);
        String storedHash = salt + ":" + otpHash;

        // Entrega o código OTP ao destinatário (canal via OtpSender)
        otpSender.send(normalized, otp);

        // Persiste
        OtpCode otpCode = OtpCode.create(normalized, storedHash, DEFAULT_TTL_MINUTES, DEFAULT_MAX_ATTEMPTS);
        otpCodeRepository.save(otpCode);

        return new OtpGenerationResult(otpCode.getId(), normalized, DEFAULT_TTL_MINUTES * 60);
    }

    /**
     * Valida OTP informado pelo usuário.
     */
    public OtpValidationResult validateOtp(String phoneE164, String inputOtp) {
        PhoneNumber phone = new PhoneNumber(phoneE164);
        String normalized = phone.getE164();

        Optional<OtpCode> otpOpt = otpCodeRepository.findLatestByPhone(normalized);
        if (otpOpt.isEmpty()) {
            return OtpValidationResult.notFound();
        }

        OtpCode otpCode = otpOpt.get();

        if (otpCode.isConsumed()) {
            return OtpValidationResult.alreadyUsed();
        }

        if (otpCode.isExpired()) {
            return OtpValidationResult.expired();
        }

        if (!otpCode.canAttempt(DEFAULT_MAX_ATTEMPTS)) {
            return OtpValidationResult.maxAttemptsExceeded();
        }

        // Verifica OTP
        String[] parts = otpCode.getOtpHash().split(":");
        if (parts.length != 2) {
            return OtpValidationResult.invalid(0, DEFAULT_MAX_ATTEMPTS);
        }
        String salt = parts[0];
        String expectedHash = parts[1];
        String inputHash = hashOtp(inputOtp, salt);

        if (!expectedHash.equals(inputHash)) {
            otpCode.recordAttempt();
            otpCodeRepository.save(otpCode);
            return OtpValidationResult.invalid(otpCode.getAttempts(), DEFAULT_MAX_ATTEMPTS);
        }

        // Sucesso - marca como consumido
        otpCode.markConsumed();
        otpCodeRepository.save(otpCode);

        return OtpValidationResult.success();
    }

    /**
     * Verifica se pode reenviar OTP (cooldown).
     */
    public boolean canResend(String phoneE164) {
        PhoneNumber phone = new PhoneNumber(phoneE164);
        String normalized = phone.getE164();

        return otpCodeRepository.findLatestByPhone(normalized)
                .map(otp -> {
                    if (otp.isConsumed() || otp.isExpired()) {
                        return true;
                    }
                    long secondsSinceCreation = java.time.Duration.between(otp.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    return secondsSinceCreation >= RESEND_COOLDOWN_SECONDS;
                })
                .orElse(true);
    }

    public int getResendCooldownSeconds() {
        return RESEND_COOLDOWN_SECONDS;
    }

    private String generateNumericOtp() {
        int otp = 100000 + secureRandom.nextInt(900000); // 6 dígitos
        return String.valueOf(otp);
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashOtp(String otp, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hash = digest.digest(otp.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    // Records para resultados
    public record OtpGenerationResult(UUID otpId, String phoneE164, int ttlSeconds) {}
    
    public static class OtpValidationResult {
        private final boolean success;
        private final String errorCode;
        private final int attempts;
        private final int maxAttempts;

        private OtpValidationResult(boolean success, String errorCode, int attempts, int maxAttempts) {
            this.success = success;
            this.errorCode = errorCode;
            this.attempts = attempts;
            this.maxAttempts = maxAttempts;
        }

        public static OtpValidationResult success() {
            return new OtpValidationResult(true, null, 0, 0);
        }

        public static OtpValidationResult notFound() {
            return new OtpValidationResult(false, "OTP_NOT_FOUND", 0, 0);
        }

        public static OtpValidationResult expired() {
            return new OtpValidationResult(false, "OTP_EXPIRED", 0, 0);
        }

        public static OtpValidationResult alreadyUsed() {
            return new OtpValidationResult(false, "OTP_ALREADY_USED", 0, 0);
        }

        public static OtpValidationResult maxAttemptsExceeded() {
            return new OtpValidationResult(false, "OTP_MAX_ATTEMPTS", 0, 0);
        }

        public static OtpValidationResult invalid(int attempts, int maxAttempts) {
            return new OtpValidationResult(false, "OTP_INVALID", attempts, maxAttempts);
        }

        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public int getAttempts() { return attempts; }
        public int getMaxAttempts() { return maxAttempts; }
    }
}