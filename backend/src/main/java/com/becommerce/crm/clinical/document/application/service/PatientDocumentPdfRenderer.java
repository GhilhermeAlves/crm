package com.becommerce.crm.clinical.document.application.service;

import com.becommerce.crm.clinical.document.domain.PatientDocument;
import com.becommerce.crm.clinical.documenttemplate.domain.DocumentType;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Gera o PDF do documento emitido (OpenPDF), com cabeçalho da empresa, título,
 * paciente, corpo preservando quebras de linha e bloco de assinatura.
 */
@Component
public class PatientDocumentPdfRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] render(PatientDocument document, String companyName, String patientName) {
        Document pdf = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph company = new Paragraph(safe(companyName), companyFont);
            company.setAlignment(Element.ALIGN_CENTER);
            pdf.add(company);
            pdf.add(new Paragraph("____________________________________________________________"));
            pdf.add(new Paragraph(" "));

            Paragraph title = new Paragraph(displayTitle(document), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            pdf.add(title);
            pdf.add(new Paragraph(" "));

            pdf.add(new Paragraph("Paciente: " + safe(patientName), bodyFont));
            pdf.add(new Paragraph(" "));

            for (String line : document.getBody().split("\\R", -1)) {
                pdf.add(new Paragraph(line.isEmpty() ? " " : line, bodyFont));
            }

            pdf.add(new Paragraph(" "));
            pdf.add(new Paragraph(" "));
            pdf.add(new Paragraph("______________________________"));
            pdf.add(new Paragraph(safe(document.getProfessionalName()), bodyFont));
            if (document.getProfessionalRegistry() != null && !document.getProfessionalRegistry().isBlank()) {
                pdf.add(new Paragraph(document.getProfessionalRegistry(), bodyFont));
            }
            pdf.add(new Paragraph("Emitido em " + DATE.format(document.getIssuedAt()), bodyFont));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar o PDF do documento do paciente.", ex);
        } finally {
            pdf.close();
        }
        return out.toByteArray();
    }

    private static String displayTitle(PatientDocument document) {
        return switch (document.getType()) {
            case PRESCRIPTION -> "RECEITUÁRIO";
            case CERTIFICATE -> "ATESTADO";
            case CONSENT -> "TERMO";
            case OTHER -> safe(document.getTitle());
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
