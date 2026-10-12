package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ColumnKind;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Money;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.RowKind;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Escreve um {@link ReportDocument} em PDF (OpenPDF): cabeçalho com título na
 * cor primária, período, empresa/CNPJ e emissão; tabelas com cabeçalho colorido
 * (repetido em quebra de página), faixas de grupo, linhas zebradas, subtotais em
 * negrito e barra de resultado; rodapé "Página X/Y" em todas as páginas.
 */
public final class PdfReportLayout {

    static final Color PRIMARY = new Color(30, 64, 175);
    private static final Color GROUP_BAND = new Color(226, 232, 248);
    private static final Color ZEBRA = new Color(247, 248, 251);
    private static final Color SUBTOTAL_BG = new Color(236, 239, 245);
    private static final Color LINE = new Color(214, 219, 228);
    private static final Color MUTED = new Color(140, 146, 158);
    private static final Color TEXT = new Color(33, 37, 41);

    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss");

    private static final Font TITLE = font(FontFactory.HELVETICA_BOLD, 14, PRIMARY);
    private static final Font META = font(FontFactory.HELVETICA, 9, MUTED);
    private static final Font META_STRONG = font(FontFactory.HELVETICA_BOLD, 10, TEXT);
    private static final Font HEADING = font(FontFactory.HELVETICA_BOLD, 11, PRIMARY);
    private static final Font HEAD = font(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
    private static final Font CELL = font(FontFactory.HELVETICA, 8.5f, TEXT);
    private static final Font CELL_ZERO = font(FontFactory.HELVETICA, 8.5f, MUTED);
    private static final Font CELL_BOLD = font(FontFactory.HELVETICA_BOLD, 8.5f, TEXT);
    private static final Font GROUP = font(FontFactory.HELVETICA_BOLD, 8.5f, PRIMARY);
    private static final Font TOTAL = font(FontFactory.HELVETICA_BOLD, 9.5f, Color.WHITE);

    private PdfReportLayout() {
    }

    public static byte[] render(ReportDocument report) {
        Rectangle size = report.landscape() ? PageSize.A4.rotate() : PageSize.A4;
        Document document = new Document(size, 36, 36, 36, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new Footer(report));
        document.open();
        header(document, report);
        for (ReportTable table : report.sections()) {
            if (table.heading() != null) {
                Paragraph heading = new Paragraph(table.heading(), HEADING);
                heading.setSpacingBefore(10);
                heading.setSpacingAfter(4);
                document.add(heading);
            }
            document.add(table(table));
        }
        document.close();
        return out.toByteArray();
    }

    private static void header(Document document, ReportDocument report) {
        PdfPTable header = new PdfPTable(new float[] {3.6f, 1.6f});
        header.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph(report.title().toUpperCase(new Locale("pt", "BR")), TITLE));
        left.addElement(new Paragraph("Período: " + report.period(), META_STRONG));
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        Paragraph company = new Paragraph(report.companyName(), META_STRONG);
        company.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(company);
        if (report.companyCnpj() != null && !report.companyCnpj().isBlank()) {
            Paragraph cnpj = new Paragraph("CNPJ " + report.companyCnpj(), META);
            cnpj.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(cnpj);
        }
        Paragraph issued = new Paragraph("Emitido em " + ISSUED.format(report.issuedAt()), META);
        issued.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(issued);
        header.addCell(left);
        header.addCell(right);

        PdfPCell rule = new PdfPCell(new Phrase(" "));
        rule.setColspan(2);
        rule.setBorder(Rectangle.BOTTOM);
        rule.setBorderColorBottom(PRIMARY);
        rule.setBorderWidthBottom(1.5f);
        rule.setFixedHeight(6);
        header.addCell(rule);
        header.setSpacingAfter(10);
        document.add(header);
    }

    private static PdfPTable table(ReportTable source) {
        List<Column> columns = source.columns();
        float[] widths = new float[columns.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = columns.get(i).width();
        }
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (Column column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column.header(), HEAD));
            cell.setBackgroundColor(PRIMARY);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(5);
            cell.setHorizontalAlignment(align(column.kind()));
            table.addCell(cell);
        }
        int itemIndex = 0;
        for (Row row : source.rows()) {
            if (row.kind() == RowKind.GROUP) {
                PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(row.cells().get(0)), GROUP));
                cell.setColspan(columns.size());
                cell.setBackgroundColor(GROUP_BAND);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(5);
                table.addCell(cell);
                itemIndex = 0;
                continue;
            }
            Color background = switch (row.kind()) {
                case TOTAL -> PRIMARY;
                case SUBTOTAL -> SUBTOTAL_BG;
                default -> itemIndex++ % 2 == 1 ? ZEBRA : Color.WHITE;
            };
            for (int i = 0; i < columns.size(); i++) {
                Object value = i < row.cells().size() ? row.cells().get(i) : null;
                PdfPCell cell = new PdfPCell(new Phrase(text(value), font(row.kind(), value)));
                cell.setBackgroundColor(background);
                cell.setBorder(Rectangle.BOTTOM);
                cell.setBorderColorBottom(LINE);
                cell.setBorderWidthBottom(0.4f);
                cell.setPadding(row.kind() == RowKind.TOTAL ? 6 : 4);
                cell.setHorizontalAlignment(align(columns.get(i).kind()));
                table.addCell(cell);
            }
        }
        return table;
    }

    private static Font font(RowKind kind, Object value) {
        return switch (kind) {
            case TOTAL -> TOTAL;
            case SUBTOTAL -> CELL_BOLD;
            default -> isZero(value) ? CELL_ZERO : CELL;
        };
    }

    private static boolean isZero(Object value) {
        return (value instanceof Money money && money.cents() == 0)
                || (value instanceof Long count && count == 0);
    }

    static String text(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Money money) {
            String formatted = FinanceLabels.formatReais(Math.abs(money.cents())).replace(' ', ' ');
            return money.cents() < 0 ? "-" + formatted : formatted;
        }
        if (value instanceof LocalDate date) {
            return FinanceLabels.dateLabel(date);
        }
        return String.valueOf(value);
    }

    private static int align(ColumnKind kind) {
        return switch (kind) {
            case MONEY, COUNT -> Element.ALIGN_RIGHT;
            case DATE -> Element.ALIGN_CENTER;
            case TEXT -> Element.ALIGN_LEFT;
        };
    }

    private static Font font(String name, float size, Color color) {
        return FontFactory.getFont(name, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED, size, Font.NORMAL, color);
    }

    /** Rodapé: empresa · Página X/Y · emitido em (o total Y é preenchido ao fechar). */
    private static final class Footer extends PdfPageEventHelper {

        private final ReportDocument report;
        private PdfTemplate total;
        private BaseFont base;

        Footer(ReportDocument report) {
            this.report = report;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 12);
            try {
                base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                throw new IllegalStateException("Fonte do rodapé indisponível", ex);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float y = document.bottom() - 22;
            canvas.setColorStroke(LINE);
            canvas.setLineWidth(0.5f);
            canvas.moveTo(document.left(), y + 12);
            canvas.lineTo(document.right(), y + 12);
            canvas.stroke();

            Font footer = font(FontFactory.HELVETICA, 7.5f, MUTED);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(report.companyName(), footer),
                    document.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Emitido em " + ISSUED.format(report.issuedAt()), footer), document.right(), y, 0);

            String page = "Página " + writer.getPageNumber() + "/";
            float center = (document.left() + document.right()) / 2;
            float width = base.getWidthPoint(page, 7.5f);
            canvas.beginText();
            canvas.setFontAndSize(base, 7.5f);
            canvas.setColorFill(MUTED);
            canvas.setTextMatrix(center - width / 2, y);
            canvas.showText(page);
            canvas.endText();
            canvas.addTemplate(total, center + width / 2, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(base, 7.5f);
            total.setColorFill(MUTED);
            total.setTextMatrix(0, 0);
            // No fechamento o contador já avançou para a página seguinte.
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }
}
