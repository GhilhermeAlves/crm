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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            Boolean sendEmail,
            /** Dados da empresa adiantados pelo CRM_ADMIN (opcional). */
            @Valid CompanyPrefill company
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
            LocalDateTime createdAt,
            CompanyPrefill company
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
            boolean hasAccount,
            CompanyPrefill company
    ) {}

    /** Dados da empresa preenchidos pelo convidado. */
    public record CompanyDetails(
            @NotBlank(message = "Razão social é obrigatória") String legalName,
            @NotBlank(message = "Nome fantasia é obrigatório") String tradingName,
            /** CNPJ ou CPF; opcional (empresas podem operar com CPF). */
            @Size(max = 20, message = "Documento muito longo") String cnpj,
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

    /**
     * Dados da empresa adiantados no convite. Todos opcionais: o convidado
     * confere e completa ao aceitar. Persistido como JSON (V126).
     */
    public record CompanyPrefill(
            @Size(max = 255) String legalName,
            @Size(max = 255) String tradingName,
            @Size(max = 20) String cnpj,
            @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @Size(max = 10) String addressZipCode,
            @Size(max = 255) String addressStreet,
            @Size(max = 20) String addressNumber,
            @Size(max = 255) String addressComplement,
            @Size(max = 120) String addressNeighborhood,
            @Size(max = 120) String addressCity,
            @Size(max = 2) String addressState
    ) {
        /** Só os campos preenchidos; vazio quando nada foi adiantado. */
        public Map<String, String> toMap() {
            Map<String, String> m = new LinkedHashMap<>();
            put(m, "legalName", legalName);
            put(m, "tradingName", tradingName);
            put(m, "cnpj", cnpj);
            put(m, "email", email);
            put(m, "phone", phone);
            put(m, "addressZipCode", addressZipCode);
            put(m, "addressStreet", addressStreet);
            put(m, "addressNumber", addressNumber);
            put(m, "addressComplement", addressComplement);
            put(m, "addressNeighborhood", addressNeighborhood);
            put(m, "addressCity", addressCity);
            put(m, "addressState", addressState);
            return m;
        }

        public static CompanyPrefill fromMap(Map<String, String> m) {
            if (m == null || m.isEmpty()) {
                return null;
            }
            return new CompanyPrefill(m.get("legalName"), m.get("tradingName"), m.get("cnpj"), m.get("email"),
                    m.get("phone"), m.get("addressZipCode"), m.get("addressStreet"), m.get("addressNumber"),
                    m.get("addressComplement"), m.get("addressNeighborhood"), m.get("addressCity"),
                    m.get("addressState"));
        }

        private static void put(Map<String, String> m, String key, String value) {
            if (value != null && !value.isBlank()) {
                m.put(key, value.trim());
            }
        }
    }

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
