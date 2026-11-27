package com.becommerce.crm.reports.export.domain.exception;

/** Exportação acima do teto de linhas: termina FAILED com a mensagem ao usuário. */
public class ReportExportTooLargeException extends RuntimeException {

    public ReportExportTooLargeException(String message) {
        super(message);
    }
}
