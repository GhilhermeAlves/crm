package com.becommerce.crm.reports.export.application.service.render;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Modelo neutro de um relatório exportado: cabeçalho (título, período, empresa,
 * emissão) e seções em tabela. É escrito uma vez por tipo de relatório e
 * consumido tanto pelo PDF ({@link PdfReportLayout}) quanto pelo Excel
 * ({@link XlsxReportWriter}).
 *
 * <p>Células aceitas: {@link String}, {@link Money} (centavos), {@link Long}
 * (contagem) e {@link LocalDate}.
 */
public record ReportDocument(
        String title,
        String period,
        String companyName,
        String companyCnpj,
        LocalDateTime issuedAt,
        boolean landscape,
        List<ReportTable> sections
) {

    public enum ColumnKind { TEXT, MONEY, COUNT, DATE }

    public enum RowKind { GROUP, ITEM, SUBTOTAL, TOTAL }

    public record Column(String header, ColumnKind kind, float width) {

        public static Column text(String header, float width) {
            return new Column(header, ColumnKind.TEXT, width);
        }

        public static Column money(String header) {
            return new Column(header, ColumnKind.MONEY, 1.4f);
        }

        public static Column count(String header) {
            return new Column(header, ColumnKind.COUNT, 0.9f);
        }

        public static Column date(String header) {
            return new Column(header, ColumnKind.DATE, 1.1f);
        }
    }

    /** Valor monetário em centavos. */
    public record Money(long cents) {}

    public record Row(RowKind kind, List<Object> cells) {

        public static Row group(String label) {
            return new Row(RowKind.GROUP, List.of(label));
        }

        public static Row item(Object... cells) {
            return new Row(RowKind.ITEM, Arrays.asList(cells));
        }

        public static Row subtotal(Object... cells) {
            return new Row(RowKind.SUBTOTAL, Arrays.asList(cells));
        }

        public static Row total(Object... cells) {
            return new Row(RowKind.TOTAL, Arrays.asList(cells));
        }
    }

    /** Uma tabela com título opcional (vira uma aba no Excel). */
    public record ReportTable(String heading, List<Column> columns, List<Row> rows) {}
}
