package com.becommerce.crm.clinical.documenttemplate.application.dto;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentTemplateResponse(
        UUID id,
        DocumentType type,
        String title,
        String body,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
