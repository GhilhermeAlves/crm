package com.becommerce.crm.masterdata.contact.registration.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Formulário público de cadastro do paciente. {@code website} é honeypot: deve
 * vir vazio (campo invisível para humanos).
 */
public record PatientRegistrationRequest(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 14) String cpf,
        LocalDate birthDate,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @Valid Address address,
        @Size(max = 200) String website) {

    public record Address(
            @Size(max = 10) String zipCode,
            @Size(max = 160) String street,
            @Size(max = 20) String number,
            @Size(max = 80) String complement,
            @Size(max = 80) String neighborhood,
            @Size(max = 80) String city,
            @Pattern(regexp = "^[A-Z]{2}$|^$", message = "UF deve ter 2 letras maiúsculas.")
            @Size(max = 2) String state) {
    }
}
