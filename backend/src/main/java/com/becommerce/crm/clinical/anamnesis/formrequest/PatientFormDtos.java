package com.becommerce.crm.clinical.anamnesis.formrequest;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** DTOs do formulário respondido pelo paciente. */
public final class PatientFormDtos {

    private PatientFormDtos() {
    }

    /** {@code mode}: LINK (enviado ao paciente) ou RECEPTION (tablet na clínica). */
    public record CreateFormRequest(@NotNull UUID modelId, String mode, Boolean sendWhatsApp) {
    }

    /** Devolvido só na criação: o token em texto puro não é guardado. */
    public record CreatedFormRequest(UUID id, UUID anamnesisId, String url, String mode,
                                     LocalDateTime expiresAt, boolean whatsAppSent) {
    }

    /** {@code status}: PENDING, ANSWERED ou EXPIRED (pendente e vencido). */
    public record FormRequestResponse(UUID id, UUID anamnesisId, String modelName, String mode,
                                      String status, LocalDateTime createdAt, LocalDateTime expiresAt,
                                      LocalDateTime answeredAt, String signatureUrl) {
    }

    /** O que a página pública mostra: só o formulário, nunca a ficha. */
    public record PublicFormResponse(String clinicName, String patientFirstName, String formName,
                                     AnamnesisModelResponse form, String mode) {
    }

    /**
     * Respostas do paciente e assinatura desenhada ({@code data:image/png;base64,...}).
     * {@code website} é honeypot (sempre vazio para humanos).
     */
    public record SubmitPublicForm(@NotNull Map<UUID, PatientAnamnesisAnswer> answers,
                                   @NotNull @Size(max = 700_000) String signature,
                                   String website) {
    }
}
