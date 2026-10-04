package com.becommerce.crm.masterdata.contact.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateContactRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @Size(max = 20) String mobile,
        @Size(max = 500) String notes,
        LocalDate birthDate,
        @Size(max = 14) String cpf,
        @Size(max = 20) String rg,
        @Size(max = 20) String rgIssuer,
        @Size(max = 20) String gender,
        @Size(max = 30) String maritalStatus,
        @Size(max = 20) String professionalStatus) {
}
