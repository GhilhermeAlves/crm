package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.port.input.PhoneAuthUseCase;
import com.becommerce.crm.application.identity.service.OtpService;
import com.becommerce.crm.infrastructure.otp.rate.OtpRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PhoneAuthControllerTest {

    private final PhoneAuthUseCase phoneAuthUseCase = mock(PhoneAuthUseCase.class);
    private final OtpService otpService = mock(OtpService.class);
    private final OtpRateLimiter otpRateLimiter = mock(OtpRateLimiter.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PhoneAuthController(phoneAuthUseCase, otpService, otpRateLimiter))
                .build();
    }

    @Test
    void sendOtp_dentroDoLimite_deveRetornar200() throws Exception {
        when(otpRateLimiter.trySend("1.2.3.4")).thenReturn(true);
        when(phoneAuthUseCase.sendOtp("+5511999999999"))
                .thenReturn(new PhoneAuthUseCase.SendOtpResult(true, "+5511999999999", 300, 60));

        mockMvc.perform(post("/api/v1/auth/phone/send-otp")
                        .header("X-Real-IP", "1.2.3.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+5511999999999\"}"))
                .andExpect(status().isOk());
        verify(phoneAuthUseCase).sendOtp("+5511999999999");
    }

    @Test
    void sendOtp_semXRealIp_deveUsarUnknown() throws Exception {
        when(otpRateLimiter.trySend("unknown")).thenReturn(true);
        when(phoneAuthUseCase.sendOtp("+5511999999999"))
                .thenReturn(new PhoneAuthUseCase.SendOtpResult(true, "+5511999999999", 300, 60));

        mockMvc.perform(post("/api/v1/auth/phone/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+5511999999999\"}"))
                .andExpect(status().isOk());
        verify(otpRateLimiter).trySend("unknown");
    }

    @Test
    void sendOtp_acimaDoLimite_deveRetornar429() throws Exception {
        when(otpRateLimiter.trySend("1.2.3.4")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/phone/send-otp")
                        .header("X-Real-IP", "1.2.3.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+5511999999999\"}"))
                .andExpect(status().isTooManyRequests());
        verify(phoneAuthUseCase, never()).sendOtp(anyString());
    }

    @Test
    void verifyOtp_dentroDoLimite_deveRetornar200() throws Exception {
        when(otpRateLimiter.tryVerify("+5511999999999")).thenReturn(true);
        when(phoneAuthUseCase.verifyOtp("+5511999999999", "123456"))
                .thenReturn(PhoneAuthUseCase.VerifyOtpResult.success(
                        "974bbedb-298d-4ec6-a037-514b24c248e4", "user@empresa.com", true));

        mockMvc.perform(post("/api/v1/auth/phone/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+5511999999999\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk());
        verify(phoneAuthUseCase).verifyOtp("+5511999999999", "123456");
    }

    @Test
    void verifyOtp_acimaDoLimite_deveRetornar429() throws Exception {
        when(otpRateLimiter.tryVerify("+5511999999999")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/phone/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+5511999999999\",\"otp\":\"123456\"}"))
                .andExpect(status().isTooManyRequests());
        verify(phoneAuthUseCase, never()).verifyOtp(anyString(), anyString());
    }
}