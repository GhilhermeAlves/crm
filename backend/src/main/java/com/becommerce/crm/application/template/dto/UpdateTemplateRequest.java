package com.becommerce.crm.application.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTemplateRequest(
        @Size(max = 120) String name,
        @Size(max = 200) String subject,
        @NotBlank String body,
        String variables
) {
}
