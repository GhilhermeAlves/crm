package com.becommerce.crm.infrastructure.identity.config;

import com.becommerce.crm.application.identity.port.output.OtpSender;
import com.becommerce.crm.application.identity.service.OtpService;
import com.becommerce.crm.domain.identity.repository.OtpCodeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpConfig {

    @Bean
    public OtpService otpService(OtpCodeRepository otpCodeRepository, OtpSender otpSender) {
        return new OtpService(otpCodeRepository, otpSender);
    }
}