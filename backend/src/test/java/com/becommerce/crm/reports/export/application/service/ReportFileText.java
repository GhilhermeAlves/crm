package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Texto legível de um arquivo exportado (PDF, CSV ou XLSX), para asserções nos testes. */
public final class ReportFileText {

    private ReportFileText() {
    }

    public static String of(ExportFormat format, byte[] content) throws Exception {
        return switch (format) {
            case CSV -> new String(content, StandardCharsets.UTF_8);
            case PDF -> {
                PdfReader reader = new PdfReader(content);
                StringBuilder text = new StringBuilder();
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
                }
                yield text.toString();
            }
            case XLSX -> {
                StringBuilder text = new StringBuilder();
                try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
                    for (Sheet sheet : workbook) {
                        for (Row row : sheet) {
                            for (Cell cell : row) {
                                text.append(cell.toString()).append('\t');
                            }
                            text.append('\n');
                        }
                    }
                }
                yield text.toString();
            }
        };
    }
}
