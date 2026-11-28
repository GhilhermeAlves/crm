package com.becommerce.crm.reports.export.application.service.render;

/**
 * Proteção contra formula injection (CSV/XLSX), padrão OWASP: texto que comece
 * com {@code = + - @}, TAB ou CR ganha um apóstrofo na frente para o Excel/
 * Planilhas não interpretar como fórmula. Use só em células de texto — números,
 * dinheiro e datas tipados não passam por aqui.
 */
public final class SpreadsheetSanitizer {

    private SpreadsheetSanitizer() {
    }

    /** Devolve o texto com {@code '} na frente quando ele poderia virar fórmula. */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
