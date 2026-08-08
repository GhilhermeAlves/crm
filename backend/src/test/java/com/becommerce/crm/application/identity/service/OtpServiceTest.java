package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.port.output.OtpSender;
import com.becommerce.crm.domain.identity.OtpCode;
import com.becommerce.crm.domain.identity.repository.OtpCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes do fluxo OTP (Sprint 7.3): entrega do código via {@link OtpSender} e
 * validação com sucesso/falha.
 */
class OtpServiceTest {

    private OtpCodeRepository repository;
    private List<OtpCode> stored;
    private List<String> deliveredCodes;

    @BeforeEach
    void setUp() {
        repository = mock(OtpCodeRepository.class);
        stored = new ArrayList<>();
        deliveredCodes = new ArrayList<>();

        when(repository.save(any(OtpCode.class))).thenAnswer(inv -> {
            OtpCode code = inv.getArgument(0);
            stored.add(code);
            return code;
        });
    }

    private OtpService serviceWith(RecordingSender sender) {
        return new OtpService(repository, sender);
    }

    @Test
    @DisplayName("generateOtp entrega o código ao OtpSender e persiste apenas hash")
    void generateOtpDeliversAndPersistsHashed() {
        RecordingSender sender = new RecordingSender();
        OtpService service = serviceWith(sender);
        when(repository.findLatestByPhone(anyString())).thenReturn(Optional.empty());

        OtpService.OtpGenerationResult result = service.generateOtp("+5511999999999");

        assertEquals("+5511999999999", result.phoneE164());
        assertEquals(300, result.ttlSeconds());
        assertEquals(1, deliveredCodes.size());
        assertTrue(deliveredCodes.get(0).matches("\\d{6}"));

        OtpCode persisted = stored.get(0);
        assertTrue(!persisted.getOtpHash().contains(deliveredCodes.get(0)));
        assertTrue(persisted.getOtpHash().contains(":"));
    }

    @Test
    @DisplayName("validateOtp aceita apenas o código entregue")
    void validateOtpAcceptsOnlyDeliveredCode() {
        RecordingSender sender = new RecordingSender();
        OtpService service = serviceWith(sender);
        when(repository.findLatestByPhone(anyString())).thenAnswer(inv -> Optional.ofNullable(lastStored()));

        service.generateOtp("+5511999999999");
        String correctCode = deliveredCodes.get(0);

        // código errado no OTP ainda não consumido -> OTP_INVALID
        assertEquals("OTP_INVALID", service.validateOtp("+5511999999999", "000000").getErrorCode());

        // código correto -> sucesso
        assertTrue(service.validateOtp("+5511999999999", correctCode).isSuccess());

        // reuso do mesmo código -> já consumido
        assertEquals("OTP_ALREADY_USED", service.validateOtp("+5511999999999", correctCode).getErrorCode());
    }

    @Test
    @DisplayName("canResend respeita o cooldown depois do envio")
    void canResendCooldown() {
        RecordingSender sender = new RecordingSender();
        OtpService service = serviceWith(sender);
        when(repository.findLatestByPhone(anyString())).thenAnswer(inv ->
                stored.isEmpty() ? Optional.empty() : Optional.of(lastStored()));

        assertTrue(service.canResend("+5511999999999"));

        service.generateOtp("+5511999999999");
        assertEquals(false, service.canResend("+5511999999999"));
    }

    private OtpCode lastStored() {
        return stored.isEmpty() ? null : stored.get(stored.size() - 1);
    }

    private final class RecordingSender implements OtpSender {
        private int sent;

        @Override
        public void send(String phoneE164, String otpCode) {
            deliveredCodes.add(otpCode);
            sent++;
        }
    }
}