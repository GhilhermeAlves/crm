package com.becommerce.crm.masterdata.company.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class CompanyMailDtos {

    private CompanyMailDtos() {
    }

    /** Sem senha na resposta: só se há uma salva. */
    public record MailSettingsResponse(boolean configured, String email, String senderName, String host,
                                       Integer port, boolean hasPassword, LocalDateTime updatedAt,
                                       InviteTemplate inviteTemplate) {
    }

    /**
     * Modelo do e-mail de convite. {@code logo} é data URL PNG/JPEG; vazio = sem
     * logo. Na mensagem, {empresa} e {perfil} são substituídos no envio.
     */
    public record InviteTemplate(
            boolean enabled,
            @Size(max = 700_000) String logo,
            @Size(max = 7) String color,
            @Size(max = 150) String title,
            @Size(max = 1500) String message,
            @Size(max = 200) String footer) {
    }

    public record InvitePreview(String subject, String html) {
    }

    /** Pedido ao Leo: tom (ACOLHEDOR, PROFISSIONAL, DESCONTRAIDO) e contexto livre. */
    public record InviteSuggestionRequest(@Size(max = 20) String tone, @Size(max = 500) String notes) {
    }

    /** Textos sugeridos pelo Leo; a empresa revisa antes de salvar. */
    public record InviteSuggestion(String title, String message, String footer) {
    }

    /** Senha em branco mantém a atual; host/porta em branco são deduzidos do e-mail. */
    public record SaveMailSettings(
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 255) String senderName,
            @Size(max = 255) String host,
            @Min(1) @Max(65535) Integer port,
            @Size(max = 500) String password) {
    }

    public record TestMail(@Email String to) {
    }
}
