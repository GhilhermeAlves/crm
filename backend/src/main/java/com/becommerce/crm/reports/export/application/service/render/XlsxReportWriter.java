package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ColumnKind;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Money;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.RowKind;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Escreve um {@link ReportDocument} em XLSX (Apache POI): uma aba por seção,
 * linha de título, período/emissão, cabeçalho em negrito com fundo, grupos e
 * subtotais em negrito, valores como número ({@code R$ #,##0.00}), datas como
 * data, larguras ajustadas e painel congelado abaixo do cabeçalho.
 */
public final class XlsxReportWriter {

    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int HEADER_ROW = 4;

    private XlsxReportWriter() {
    }

    public static byte[] write(ReportDocument report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            for (ReportTable table : report.sections()) {
                String name = table.heading() != null ? table.heading() : "Relatório";
                XSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(
                        report.sections().size() > 1 ? name : "Relatório"));
                sheet(sheet, report, table, styles);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void sheet(XSSFSheet sheet, ReportDocument report, ReportTable table, Styles styles) {
        List<Column> columns = table.columns();
        int lastColumn = columns.size() - 1;

        Cell title = sheet.createRow(0).createCell(0);
        title.setCellValue(report.title());
        title.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(lastColumn, 1)));

        sheet.createRow(1).createCell(0).setCellValue(report.companyName()
                + (report.companyCnpj() == null || report.companyCnpj().isBlank() ? "" : " · CNPJ "
                + report.companyCnpj()));
        sheet.createRow(2).createCell(0).setCellValue("Período: " + report.period()
                + "   ·   Emitido em " + ISSUED.format(report.issuedAt()));
        sheet.getRow(1).getCell(0).setCellStyle(styles.meta);
        sheet.getRow(2).getCell(0).setCellStyle(styles.meta);

        org.apache.poi.ss.usermodel.Row head = sheet.createRow(HEADER_ROW);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = head.createCell(i);
            cell.setCellValue(columns.get(i).header());
            cell.setCellStyle(styles.head);
        }

        int rowIndex = HEADER_ROW + 1;
        for (Row row : table.rows()) {
            org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowIndex);
            if (row.kind() == RowKind.GROUP) {
                for (int i = 0; i < columns.size(); i++) {
                    excelRow.createCell(i).setCellStyle(styles.group);
                }
                excelRow.getCell(0).setCellValue(String.valueOf(row.cells().get(0)));
                if (lastColumn > 0) {
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
                }
            } else {
                for (int i = 0; i < columns.size(); i++) {
                    Object value = i < row.cells().size() ? row.cells().get(i) : null;
                    Cell cell = excelRow.createCell(i);
                    write(cell, value);
                    cell.setCellStyle(styles.of(row.kind(), columns.get(i).kind(), value));
                }
            }
            rowIndex++;
        }

        sheet.createFreezePane(0, HEADER_ROW + 1);
        // Largura pelo maior texto da coluna (autoSizeColumn depende de fontes AWT,
        // ausentes na imagem do servidor).
        for (int i = 0; i < columns.size(); i++) {
            int chars = columns.get(i).header().length();
            for (Row row : table.rows()) {
                if (row.kind() != RowKind.GROUP && i < row.cells().size()) {
                    chars = Math.max(chars, displayLength(row.cells().get(i)));
                }
            }
            int min = columns.get(i).kind() == ColumnKind.MONEY ? 16 : 12;
            sheet.setColumnWidth(i, Math.min(Math.max(chars + 2, min), 60) * 256);
        }
    }

    private static int displayLength(Object value) {
        if (value instanceof Money) {
            return PdfReportLayout.text(value).length();
        }
        if (value instanceof LocalDate) {
            return 10;
        }
        return value == null ? 0 : String.valueOf(value).length();
    }

    private static void write(Cell cell, Object value) {
        if (value instanceof Money money) {
            cell.setCellValue(money.cents() / 100.0);
        } else if (value instanceof Long count) {
            cell.setCellValue(count);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
        } else if (value != null) {
            cell.setCellValue(String.valueOf(value));
        }
    }

    /** Estilos criados uma vez por planilha (o Excel limita a quantidade). */
    private static final class Styles {

        private final XSSFCellStyle title;
        private final XSSFCellStyle meta;
        private final XSSFCellStyle head;
        private final XSSFCellStyle group;
        private final Map<RowKind, Map<ColumnKind, XSSFCellStyle>> cells = new EnumMap<>(RowKind.class);
        private final Map<ColumnKind, XSSFCellStyle> zeros = new EnumMap<>(ColumnKind.class);

        Styles(XSSFWorkbook workbook) {
            XSSFColor primary = color(PdfReportLayout.PRIMARY);
            XSSFColor band = color(new java.awt.Color(226, 232, 248));
            XSSFColor subtotal = color(new java.awt.Color(236, 239, 245));
            XSSFColor muted = color(new java.awt.Color(140, 146, 158));
            short money = workbook.createDataFormat().getFormat("\"R$\" #,##0.00;-\"R$\" #,##0.00");
            short date = workbook.createDataFormat().getFormat("dd/mm/yyyy");
            short count = workbook.createDataFormat().getFormat("0");

            title = workbook.createCellStyle();
            title.setFont(font(workbook, true, 14, primary));
            meta = workbook.createCellStyle();
            meta.setFont(font(workbook, false, 10, muted));

            head = workbook.createCellStyle();
            head.setFont(font(workbook, true, 10, color(java.awt.Color.WHITE)));
            head.setFillForegroundColor(primary);
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            group = workbook.createCellStyle();
            group.setFont(font(workbook, true, 10, primary));
            group.setFillForegroundColor(band);
            group.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont regular = font(workbook, false, 10, null);
            XSSFFont bold = font(workbook, true, 10, null);
            XSSFFont white = font(workbook, true, 11, color(java.awt.Color.WHITE));
            XSSFFont grey = font(workbook, false, 10, muted);
            for (RowKind kind : List.of(RowKind.ITEM, RowKind.SUBTOTAL, RowKind.TOTAL)) {
                Map<ColumnKind, XSSFCellStyle> byColumn = new EnumMap<>(ColumnKind.class);
                for (ColumnKind columnKind : ColumnKind.values()) {
                    XSSFCellStyle style = workbook.createCellStyle();
                    style.setFont(kind == RowKind.ITEM ? regular : kind == RowKind.SUBTOTAL ? bold : white);
                    applyFormat(style, columnKind, money, date, count);
                    style.setBorderBottom(BorderStyle.HAIR);
                    if (kind != RowKind.ITEM) {
                        style.setFillForegroundColor(kind == RowKind.TOTAL ? primary : subtotal);
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }
                    byColumn.put(columnKind, style);
                }
                cells.put(kind, byColumn);
            }
            for (ColumnKind columnKind : ColumnKind.values()) {
                XSSFCellStyle style = workbook.createCellStyle();
                style.setFont(grey);
                applyFormat(style, columnKind, money, date, count);
                style.setBorderBottom(BorderStyle.HAIR);
                zeros.put(columnKind, style);
            }
        }

        CellStyle of(RowKind kind, ColumnKind column, Object value) {
            boolean zero = (value instanceof Money m && m.cents() == 0) || (value instanceof Long l && l == 0);
            if (kind == RowKind.ITEM && zero) {
                return zeros.get(column);
            }
            return cells.get(kind).get(column);
        }

        private static void applyFormat(XSSFCellStyle style, ColumnKind kind, short money, short date,
                                        short count) {
            switch (kind) {
                case MONEY -> {
                    style.setDataFormat(money);
                    style.setAlignment(HorizontalAlignment.RIGHT);
                }
                case DATE -> {
                    style.setDataFormat(date);
                    style.setAlignment(HorizontalAlignment.CENTER);
                }
                case COUNT -> {
                    style.setDataFormat(count);
                    style.setAlignment(HorizontalAlignment.RIGHT);
                }
                case TEXT -> style.setAlignment(HorizontalAlignment.LEFT);
            }
        }

        private static XSSFFont font(XSSFWorkbook workbook, boolean bold, int size, XSSFColor color) {
            XSSFFont font = workbook.createFont();
            font.setBold(bold);
            font.setFontHeightInPoints((short) size);
            if (color != null) {
                font.setColor(color);
            }
            return font;
        }

        private static XSSFColor color(java.awt.Color color) {
            return new XSSFColor(color, null);
        }
    }
}
