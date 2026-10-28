package com.becommerce.crm.communication.notification.application;

/**
 * Abstração de envio de e-mail (Sprint 8.5). Em dev/usado o
 * {@code ConsoleEmailSender} (apenas loga). Em produção, trocar o bean por um
 * provider real (SMTP/Resend/SES) não exige segredos no Git.
 */
public interface EmailSender {

    void sendInvitation(String to, String companyName, String role, String tokenUrl);

    /** False no envio de desenvolvimento (só loga): a tela avisa que o e-mail não sai. */
    default boolean deliversEmail() {
        return false;
    }

    /**
     * Link do formulário para o paciente. O remetente aparece com o nome da
     * clínica e as respostas vão para {@code replyTo} (e-mail da clínica ou de
     * quem enviou); o endereço técnico de envio é o da conta SMTP.
     */
    default void sendPatientForm(String to, String clinicName, String replyTo, String patientName,
                                 String formName, String url) {
        throw new UnsupportedOperationException("Envio de formulário por e-mail não suportado");
    }
}