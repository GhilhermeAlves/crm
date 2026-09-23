package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.port.input.PhoneAuthUseCase;
import com.becommerce.crm.application.identity.service.OtpService;
import com.becommerce.crm.domain.identity.valueobject.PhoneNumber;
import com.becommerce.crm.infrastructure.otp.rate.OtpRateLimiter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints para autenticação por telefone/OTP (Sprint 7.3).
 */
@RestController
@RequestMapping("/api/v1/auth/phone")
public class PhoneAuthController {

    private static final int TOO_MANY_REQUESTS = 429;

    private final PhoneAuthUseCase phoneAuthUseCase;
    private final OtpService otpService;
    private final OtpRateLimiter otpRateLimiter;

    public PhoneAuthController(PhoneAuthUseCase phoneAuthUseCase, OtpService otpService, OtpRateLimiter otpRateLimiter) {
        this.phoneAuthUseCase = phoneAuthUseCase;
        this.otpService = otpService;
        this.otpRateLimiter = otpRateLimiter;
    }

    /**
     * Solicita envio de OTP para o telefone informado.
     */
    @PostMapping(value = "/send-otp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        String ip = realIp != null && !realIp.isBlank() ? realIp : "unknown";
        if (!otpRateLimiter.trySend(ip)) {
            return ResponseEntity.status(TOO_MANY_REQUESTS).build();
        }
        PhoneAuthUseCase.SendOtpResult result = phoneAuthUseCase.sendOtp(request.phone());
        if (!result.sent()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Valida OTP e autentica usuário.
     */
    @PostMapping(value = "/verify-otp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        if (!otpRateLimiter.tryVerify(request.phone())) {
            return ResponseEntity.status(TOO_MANY_REQUESTS).build();
        }
        PhoneAuthUseCase.VerifyOtpResult result = phoneAuthUseCase.verifyOtp(request.phone(), request.otp());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Verifica se pode reenviar OTP (cooldown).
     */
    @PostMapping(value = "/can-resend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CanResendResponse> canResend(@Valid @RequestBody PhoneRequest request) {
        try {
            PhoneNumber phone = new PhoneNumber(request.phone());
            boolean canResend = otpService.canResend(phone.getE164());
            return ResponseEntity.ok(new CanResendResponse(canResend, otpService.getResendCooldownSeconds()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CanResendResponse(false, 0));
        }
    }

    public record SendOtpRequest(@NotBlank String phone) {}
    public record VerifyOtpRequest(@NotBlank String phone, @NotBlank String otp) {}
    public record PhoneRequest(@NotBlank String phone) {}
    public record CanResendResponse(boolean canResend, int cooldownSeconds) {}
}