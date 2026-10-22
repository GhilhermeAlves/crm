package com.becommerce.crm.clinical.document.application.service;

import com.becommerce.crm.clinical.document.domain.PatientDocument;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PatientDocumentPdfRendererTest {

    private final PatientDocumentPdfRenderer renderer = new PatientDocumentPdfRenderer();

    @Test
    void rendersValidPdfWithSignatureAndBody() {
        PatientDocument document = PatientDocument.issue(UUID.randomUUID(), UUID.randomUUID(), null,
                DocumentType.PRESCRIPTION, "Receita", "Dipirona 500mg\n1 comprimido a cada 6 horas.",
                "Dra. Ana Souza", "CRM 12345", UUID.randomUUID());

        byte[] pdf = renderer.render(document, "Clínica Bem", "Maria Silva");

        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}
