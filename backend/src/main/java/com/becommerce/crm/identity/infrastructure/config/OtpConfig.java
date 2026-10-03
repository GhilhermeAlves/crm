package com.becommerce.crm.identity.infrastructure.config;

import com.becommerce.crm.identity.application.port.output.OtpSender;
import com.becommerce.crm.identity.application.service.OtpService;
import com.becommerce.crm.identity.domain.repository.OtpCodeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfig {

    @Bean
    public OtpService otpService(OtpCodeRepository otpCodeRepository, OtpSender otpSender) {
        return new OtpService(otpCodeRepository, otpSender);
    }
}