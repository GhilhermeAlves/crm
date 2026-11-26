package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.trichology.domain.AssessmentData.MarkerScore;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.ReportSection;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.RpsSummary;
import com.becommerce.crm.clinical.trichology.domain.AssessmentData.SystemScore;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF do relatório tricológico (doc v1.2, seção 4.2), no padrão do
 * {@code PatientDocumentPdfRenderer}: seções em texto, fotos em grade por
 * região, quadro RPS e rodapé com aviso, autoria e código de verificação.
 */
@Component
public class TrichologyReportPdfRenderer {

    static final String DISCLAIMER = "Relatório de apoio à decisão clínica. Conteúdo sugerido por inteligência "
            + "artificial foi revisado e aprovado pelo profissional responsável. Não substitui avaliação médica "
            + "nem prescrição.";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Sao_Paulo"));
    /** Tons de cinza (0 = preto, 1 = branco): java.awt é proibido pelo Checkstyle. */
    private static final GrayColor GRID = new GrayColor(0.8f);
    private static final Float HEADER = 0.93f;

    /** Foto da grade: bytes já baixados do storage. */
    public record Photo(String regionLabel, String magnification, String caption, byte[] data) {
    }

    public record Input(String clinicName, String patientName, String professionalName, String professionalRegistry,
                        int reportVersion, Instant issuedAt, List<ReportSection> sections, List<Photo> photos,
                        RpsSummary rps, String verificationCode, String verificationUrl) {
    }

    public byte[] render(Input in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(pdf, out);
            pdf.open();
            Font clinic = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph p = new Paragraph(safe(in.clinicName()), clinic);
            p.setAlignment(Element.ALIGN_CENTER);
            pdf.add(p);
            p = new Paragraph("RELATÓRIO DE AVALIAÇÃO TRICOLÓGICA"
                    + (in.reportVersion() > 1 ? " — versão " + in.reportVersion() : ""), title);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingBefore(6);
            p.setSpacingAfter(4);
            pdf.add(p);
            pdf.add(new Paragraph("Paciente: " + safe(in.patientName()) + "    Emissão: "
                    + DATE.format(in.issuedAt()), body));

            for (ReportSection section : in.sections()) {
                Paragraph h = new Paragraph(safe(section.title()), heading);
                h.setSpacingBefore(12);
                h.setSpacingAfter(3);
                pdf.add(h);
                if ("labs".equals(section.key()) && in.rps() != null && !in.rps().markers().isEmpty()) {
                    pdf.add(rpsTable(in.rps(), body));
                }
                for (String line : safe(section.content()).split("\\R")) {
                    pdf.add(new Paragraph(line, body));
                }
                if ("trichoscopy".equals(section.key()) && !in.photos().isEmpty()) {
                    pdf.add(photoGrid(in.photos(), small));
                }
            }

            Paragraph sign = new Paragraph("\n" + safe(in.professionalName())
                    + (in.professionalRegistry() == null ? "" : " — " + in.professionalRegistry()), body);
            sign.setAlignment(Element.ALIGN_CENTER);
            sign.setSpacingBefore(18);
            pdf.add(sign);

            Paragraph footer = new Paragraph();
            footer.setSpacingBefore(14);
            footer.add(new Chunk(DISCLAIMER + "\n", small));
            footer.add(new Chunk("Código de verificação: " + in.verificationCode()
                    + (in.verificationUrl() == null || in.verificationUrl().isBlank() ? ""
                    : " — " + in.verificationUrl()), small));
            pdf.add(footer);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar o PDF do relatório tricológico", e);
        } finally {
            pdf.close();
        }
        return out.toByteArray();
    }

    private static PdfPTable rpsTable(RpsSummary rps, Font font) {
        PdfPTable table = new PdfPTable(new float[]{3.2f, 1.6f, 2.2f, 1.2f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);
        for (String h : List.of("Marcador", "Resultado", "Referência", "Quartil", "Nota")) {
            table.addCell(cell(h, font, HEADER));
        }
        for (MarkerScore m : rps.markers()) {
            table.addCell(cell(m.label(), font, null));
            table.addCell(cell(m.value().stripTrailingZeros().toPlainString() + " " + safe(m.unit()), font, null));
            table.addCell(cell(m.referenceMin() == null ? "—" : m.referenceMin().stripTrailingZeros().toPlainString()
                    + " a " + m.referenceMax().stripTrailingZeros().toPlainString(), font, null));
            String quartile = switch (m.position()) {
                case "LOW" -> "abaixo";
                case "HIGH" -> "acima";
                case "IN_RANGE" -> "Q" + m.quartile() + (m.targetQuartile() == null ? "" : " (meta Q"
                        + m.targetQuartile() + ")");
                default -> "—";
            };
            table.addCell(cell(quartile, font, null));
            table.addCell(cell(String.valueOf(m.score()), font, null));
        }
        StringBuilder systems = new StringBuilder("RPS geral: " + rps.overallScore() + " — ");
        for (SystemScore s : rps.systems()) {
            systems.append(s.system()).append(' ').append(s.score()).append("  ");
        }
        PdfPCell total = cell(systems.toString().trim(), font, HEADER);
        total.setColspan(5);
        table.addCell(total);
        return table;
    }

    private static PdfPTable photoGrid(List<Photo> photos, Font font) throws Exception {
        PdfPTable grid = new PdfPTable(3);
        grid.setWidthPercentage(100);
        grid.setSpacingBefore(6);
        for (Photo photo : photos) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(GRID);
            cell.setPadding(4);
            Image image = Image.getInstance(photo.data());
            image.scaleToFit(150, 120);
            cell.addElement(image);
            cell.addElement(new Phrase(photo.regionLabel()
                    + (photo.magnification() == null ? "" : " · " + photo.magnification())
                    + (photo.caption() == null || photo.caption().isBlank() ? "" : "\n" + photo.caption()), font));
            grid.addCell(cell);
        }
        int remainder = photos.size() % 3;
        for (int i = 0; remainder != 0 && i < 3 - remainder; i++) {
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorder(0);
            grid.addCell(empty);
        }
        return grid;
    }

    private static PdfPCell cell(String text, Font font, Float background) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setBorderColor(GRID);
        cell.setPadding(3);
        if (background != null) {
            cell.setGrayFill(background);
        }
        return cell;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static String plainText(List<ReportSection> sections) {
        StringBuilder sb = new StringBuilder();
        for (ReportSection s : sections) {
            sb.append(s.title().toUpperCase()).append('\n').append(safe(s.content())).append("\n\n");
        }
        return sb.toString().trim();
    }
}
