package com.becommerce.crm.reports.export.domain.exception;

/** Limite de exportações simultâneas ou por hora do usuário excedido — 429. */
public class ReportExportRateLimitException extends RuntimeException {

    public ReportExportRateLimitException(String message) {
        super(message);
    }
}
