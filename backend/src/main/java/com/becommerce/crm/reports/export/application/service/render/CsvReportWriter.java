package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Money;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.RowKind;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Escreve um {@link ReportDocument} em CSV para abrir no Excel/Planilhas em
 * pt-BR: UTF-8 com BOM, separador {@code ;}, decimal com vírgula e sem "R$"
 * (os valores continuam somáveis). Cada seção vem com o próprio título e
 * cabeçalho de colunas, separada por uma linha em branco.
 */
public final class CsvReportWriter {

    static final char SEPARATOR = ';';
    private static final String NEWLINE = "\r\n";
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private CsvReportWriter() {
    }

    public static byte[] write(ReportDocument report) {
        StringBuilder csv = new StringBuilder();
        line(csv, List.of(report.title()));
        line(csv, List.of("Período", report.period()));
        line(csv, List.of("Empresa", report.companyName()));
        if (report.companyCnpj() != null && !report.companyCnpj().isBlank()) {
            line(csv, List.of("CNPJ", report.companyCnpj()));
        }
        line(csv, List.of("Emitido em", ISSUED.format(report.issuedAt())));
        for (ReportTable table : report.sections()) {
            csv.append(NEWLINE);
            if (table.heading() != null) {
                line(csv, List.of(table.heading()));
            }
            line(csv, table.columns().stream().map(Column::header).toList());
            for (Row row : table.rows()) {
                if (row.kind() == RowKind.GROUP) {
                    line(csv, List.of(String.valueOf(row.cells().get(0))));
                    continue;
                }
                List<String> cells = new ArrayList<>();
                for (int i = 0; i < table.columns().size(); i++) {
                    cells.add(text(i < row.cells().size() ? row.cells().get(i) : null));
                }
                line(csv, cells);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(UTF8_BOM);
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /** Valor como planilha entende: 1234,56 / 03/11/2026 / texto. */
    static String text(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Money money) {
            long abs = Math.abs(money.cents());
            String number = (abs / 100) + "," + String.format("%02d", abs % 100);
            return money.cents() < 0 ? "-" + number : number;
        }
        if (value instanceof LocalDate date) {
            return FinanceLabels.dateLabel(date);
        }
        return String.valueOf(value);
    }

    private static void line(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(escape(cells.get(i)));
        }
        csv.append(NEWLINE);
    }

    /** Aspas quando o campo tem separador, aspas ou quebra de linha (RFC 4180). */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
