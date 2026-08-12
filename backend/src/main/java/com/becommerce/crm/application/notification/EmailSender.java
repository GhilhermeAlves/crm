package com.becommerce.crm.application.notification;

/**
 * Abstração de envio de e-mail (Sprint 8.5). Em dev/usado o
 * {@code ConsoleEmailSender} (apenas loga). Em produção, trocar o bean por um
 * provider real (SMTP/Resend/SES) não exige segredos no Git.
 */
public interface EmailSender {

    void sendInvitation(String to, String companyName, String role, String tokenUrl);
}