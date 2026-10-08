package com.becommerce.crm.communication.omnichannel.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IgnoredContactRequest(
        @NotBlank
        @Pattern(regexp = "^[+\\d\\s().-]{10,40}$", message = "Telefone inválido")
        String phone,
        @Size(max = 120) String label) {
}
