package com.becommerce.crm.clinical.document.application.dto;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientDocumentResponse(
        UUID id,
        UUID contactId,
        UUID templateId,
        DocumentType type,
        String title,
        String body,
        String professionalName,
        String professionalRegistry,
        UUID issuedBy,
        String issuedByName,
        LocalDateTime issuedAt) {
}
