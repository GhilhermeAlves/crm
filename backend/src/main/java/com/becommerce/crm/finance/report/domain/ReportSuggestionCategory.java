package com.becommerce.crm.finance.report.domain;

import java.util.Locale;

/**
 * Temas que o usuário pode marcar no diálogo "Sugerir um relatório". O rótulo
 * pt-BR vive no frontend; aqui interessa apenas o contrato estável do backend.
 */
public enum ReportSuggestionCategory {
    CLINICAL_RECORD,
    INVENTORY,
    FINANCE,
    OTHER;

    public static ReportSuggestionCategory from(String raw) {
        return ReportSuggestionCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
