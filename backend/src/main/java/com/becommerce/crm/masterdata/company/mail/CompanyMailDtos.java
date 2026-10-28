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
                                       Integer port, boolean hasPassword, LocalDateTime updatedAt) {
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
