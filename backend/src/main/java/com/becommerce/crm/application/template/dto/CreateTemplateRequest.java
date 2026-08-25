package com.becommerce.crm.application.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTemplateRequest(
        @NotBlank @Size(max = 120) String name,
        String channelType,
        @Size(max = 200) String subject,
        @NotBlank String body,
        String variables,
        @Size(max = 120) String externalTemplateId
) {
}
