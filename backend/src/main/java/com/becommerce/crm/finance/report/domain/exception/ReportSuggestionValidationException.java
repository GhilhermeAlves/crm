package com.becommerce.crm.finance.report.domain.exception;

/** Sugestão de relatório inválida (resposta 400). */
public class ReportSuggestionValidationException extends RuntimeException {

    public ReportSuggestionValidationException(String message) {
        super(message);
    }
}
