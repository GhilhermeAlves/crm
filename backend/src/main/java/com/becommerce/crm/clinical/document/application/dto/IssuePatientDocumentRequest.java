package com.becommerce.crm.clinical.document.application.dto;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record IssuePatientDocumentRequest(
        UUID templateId,
        @NotNull DocumentType type,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 20000) String body,
        @NotBlank @Size(max = 160) String professionalName,
        @Size(max = 40) String professionalRegistry) {
}
