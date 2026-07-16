package com.becommerce.crm.application.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCompanyRequest(
        @NotBlank(message = "Razão social é obrigatória")
        String legalName,

        @NotBlank(message = "Nome fantasia é obrigatório")
        String tradingName,

        @NotBlank(message = "CNPJ é obrigatório")
        String cnpj,

        String stateRegistration,

        String municipalRegistration,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        String phone,

        String website,

        @NotBlank(message = "CEP é obrigatório")
        String addressZipCode,

        @NotBlank(message = "Rua é obrigatória")
        String addressStreet,

        @NotBlank(message = "Número é obrigatório")
        String addressNumber,

        String addressComplement,

        @NotBlank(message = "Bairro é obrigatório")
        String addressNeighborhood,

        @NotBlank(message = "Cidade é obrigatória")
        String addressCity,

        @NotBlank(message = "Estado é obrigatório")
        String addressState,

        String addressCountry,

        @NotNull(message = "Plano é obrigatório")
        String plan,

        Integer maxUsers,

        Integer maxStorageMb,

        String logoUrl,

        String notes
) {
}
