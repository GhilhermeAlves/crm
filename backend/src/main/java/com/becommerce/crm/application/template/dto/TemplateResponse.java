package com.becommerce.crm.application.template.dto;

import com.becommerce.crm.domain.template.TemplateStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        UUID companyId,
        String name,
        String channelType,
        String subject,
        String body,
        List<String> variables,
        TemplateStatus status,
        int version,
        String externalTemplateId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
