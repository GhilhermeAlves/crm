package com.becommerce.crm.sales.scheduling.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BirthdayMessageSettingsDto(
        boolean enabled,
        @NotBlank @Size(max = 2000) String template
) {}
