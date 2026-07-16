package com.becommerce.crm.application.identity.port.output;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
}
