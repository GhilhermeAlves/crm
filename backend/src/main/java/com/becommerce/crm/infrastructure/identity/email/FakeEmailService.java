package com.becommerce.crm.infrastructure.identity.email;

import com.becommerce.crm.application.identity.port.output.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FakeEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(FakeEmailService.class);

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("=== FAKE EMAIL SERVICE ===");
        log.info("Sending password reset email to: {}", to);
        log.info("Reset token: {}", token);
        log.info("Reset link: http://localhost:3000/reset-password?token={}", token);
        log.info("=== END FAKE EMAIL ===");
    }
}
