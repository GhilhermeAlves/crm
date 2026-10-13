package com.becommerce.crm.masterdata.contact.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateContactRequest(
        @NotBlank(message = "Primeiro nome é obrigatório.")
        @Size(max = 100)
        String firstName,
        @Size(max = 100)
        String lastName,
        @Email()
        @Size(max = 255)
        String email,
        @Size(max = 20)
        String phone,
        @Size(max = 20)
        String mobile,
        @Size(max = 500)
        String notes,
        LocalDate birthDate,
        @Size(max = 14)
        String cpf,
        @Size(max = 20)
        String rg,
        @Size(max = 20)
        String rgIssuer,
        @Size(max = 20)
        String gender,
        @Size(max = 30)
        String maritalStatus,
        @Size(max = 20)
        String professionalStatus,
        @Size(max = 10)
        String zipCode,
        @Size(max = 160)
        String street,
        @Size(max = 20)
        String addressNumber,
        @Size(max = 80)
        String addressComplement,
        @Size(max = 80)
        String neighborhood,
        @Size(max = 80)
        String city,
        @Pattern(regexp = "^[A-Z]{2}$|^$", message = "UF deve ter 2 letras maiúsculas.")
        @Size(max = 2)
        String state,
        @Size(max = 80)
        String healthPlan,
        @Size(max = 40)
        String healthPlanNumber,
        @Size(max = 40)
        String recordNumber,
        @Size(max = 80)
        String referralSource,
        Boolean hasGuardian,
        @Size(max = 160)
        String guardianName,
        @Size(max = 14)
        String guardianCpf,
        @Size(max = 20)
        String guardianPhone
) {}
