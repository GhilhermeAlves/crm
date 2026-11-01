package com.becommerce.crm.identity.invitation.application.dto;

import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Contratos do convite de criação de empresa (V123). */
public final class CompanyInvitationDtos {

    private CompanyInvitationDtos() {
    }

    /** CRM_ADMIN: e-mail do convidado e o que a empresa terá. */
    public record CreateRequest(
            @NotBlank @Email String email,
            @Size(max = 255) String name,
            @NotBlank String segment,
            @NotEmpty List<String> enabledModules,
            @NotBlank String plan,
            @NotNull @Min(1) Integer maxUsers,
            Boolean sendEmail
    ) {
        public boolean shouldSendEmail() {
            return sendEmail == null || sendEmail;
        }
    }

    public record Response(
            UUID id,
            String email,
            String inviteeName,
            String segment,
            List<String> enabledModules,
            String plan,
            int maxUsers,
            InvitationStatus status,
            UUID companyId,
            UUID invitedBy,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {}

    /** Convite com link recém-gerado. O link anterior deixa de valer. */
    public record LinkResponse(Response invitation, String url) {}

    /** Prévia pública em /convite-empresa/{token}. */
    public record PreviewResponse(
            String email,
            String inviteeName,
            String segment,
            List<String> enabledModules,
            String plan,
            int maxUsers,
            InvitationStatus status,
            LocalDateTime expiresAt,
            boolean hasAccount
    ) {}

    /** Dados da empresa preenchidos pelo convidado. */
    public record CompanyDetails(
            @NotBlank(message = "Razão social é obrigatória") String legalName,
            @NotBlank(message = "Nome fantasia é obrigatório") String tradingName,
            @NotBlank(message = "CNPJ é obrigatório") String cnpj,
            @NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String email,
            @NotBlank(message = "Telefone é obrigatório") String phone,
            @NotBlank(message = "CEP é obrigatório") String addressZipCode,
            @NotBlank(message = "Rua é obrigatória") String addressStreet,
            @NotBlank(message = "Número é obrigatório") String addressNumber,
            String addressComplement,
            @NotBlank(message = "Bairro é obrigatório") String addressNeighborhood,
            @NotBlank(message = "Cidade é obrigatória") String addressCity,
            @NotBlank(message = "Estado é obrigatório") String addressState
    ) {}

    /** Quem já tem conta: entra e aceita informando os dados da empresa. */
    public record AcceptRequest(
            @NotBlank String token,
            @Valid @NotNull CompanyDetails company
    ) {}

    /** Quem ainda não tem conta. O e-mail é sempre o do convite. */
    public record RegisterRequest(
            @NotBlank String token,
            @NotBlank @Size(max = 255) String name,
            @NotBlank String password,
            @Valid @NotNull CompanyDetails company
    ) {
        @Override
        public String toString() {
            return "RegisterRequest[name=" + name + "]";
        }
    }
}
