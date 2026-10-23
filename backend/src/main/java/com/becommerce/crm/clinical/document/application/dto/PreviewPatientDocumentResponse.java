package com.becommerce.crm.clinical.document.application.dto;

import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;

public record PreviewPatientDocumentResponse(DocumentType type, String title, String body) {
}
