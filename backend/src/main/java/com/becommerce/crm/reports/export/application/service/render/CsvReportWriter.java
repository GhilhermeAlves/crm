package com.becommerce.crm.reports.export.application.service.render;

import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Column;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Money;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.ReportTable;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.Row;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument.RowKind;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
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
 *
 * <p>Escreve linha a linha num {@link BufferedWriter} sobre o
 * {@link OutputStream} de destino, sem montar o CSV inteiro num
 * {@code StringBuilder} (evita texto e bytes duplicados em memória).
 */
public final class CsvReportWriter {

    static final char SEPARATOR = ';';
    private static final String NEWLINE = "\r\n";
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private CsvReportWriter() {
    }

    public static byte[] write(ReportDocument report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(report, out);
        return out.toByteArray();
    }

    /** Escreve direto no {@code out} (faz flush, mas não o fecha). */
    public static void write(ReportDocument report, OutputStream out) {
        try {
            out.write(UTF8_BOM);
            Writer csv = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 64 * 1024);
            body(csv, report);
            csv.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void body(Writer csv, ReportDocument report) throws IOException {
        metaLine(csv, List.of(report.title()));
        metaLine(csv, List.of("Período", report.period()));
        metaLine(csv, List.of("Empresa", report.companyName()));
        if (report.companyCnpj() != null && !report.companyCnpj().isBlank()) {
            metaLine(csv, List.of("CNPJ", report.companyCnpj()));
        }
        metaLine(csv, List.of("Emitido em", ISSUED.format(report.issuedAt())));
        if (report.notice() != null) {
            csv.write(NEWLINE);
            metaLine(csv, List.of(report.notice()));
        }
        for (ReportTable table : report.sections()) {
            csv.write(NEWLINE);
            if (table.heading() != null) {
                metaLine(csv, List.of(table.heading()));
            }
            metaLine(csv, table.columns().stream().map(Column::header).toList());
            for (Row row : table.rows()) {
                if (row.kind() == RowKind.GROUP) {
                    line(csv, List.of(text(row.cells().get(0))));
                    continue;
                }
                List<String> cells = new ArrayList<>();
                for (int i = 0; i < table.columns().size(); i++) {
                    cells.add(text(i < row.cells().size() ? row.cells().get(i) : null));
                }
                line(csv, cells);
            }
        }
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
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        // Texto livre (nome de paciente, descrição...) pode carregar fórmula.
        return SpreadsheetSanitizer.sanitize(String.valueOf(value));
    }

    /** Linha de cabeçalho/metadados: tudo é texto, então sempre sanitiza. */
    private static void metaLine(Writer csv, List<String> cells) throws IOException {
        line(csv, cells.stream().map(SpreadsheetSanitizer::sanitize).toList());
    }

    private static void line(Writer csv, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.write(SEPARATOR);
            }
            csv.write(escape(cells.get(i)));
        }
        csv.write(NEWLINE);
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
