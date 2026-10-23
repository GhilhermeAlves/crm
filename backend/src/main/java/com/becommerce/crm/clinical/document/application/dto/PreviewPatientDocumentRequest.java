package com.becommerce.crm.clinical.document.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PreviewPatientDocumentRequest(@NotNull UUID templateId) {
}
