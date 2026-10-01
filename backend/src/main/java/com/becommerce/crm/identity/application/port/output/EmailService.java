package com.becommerce.crm.identity.application.port.output;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
}
