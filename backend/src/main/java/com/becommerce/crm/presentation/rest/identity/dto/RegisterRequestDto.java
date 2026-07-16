package com.becommerce.crm.presentation.rest.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RegisterRequestDto(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String name,
    UUID companyId
) {
}
