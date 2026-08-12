package com.becommerce.crm.application.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
        @Size(max = 500)
        String notes
) {}