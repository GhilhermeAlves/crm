package com.becommerce.crm.reports.export.domain.exception;

/** Pedido de exportação inválido (tipo, formato ou período) — 400. */
public class ReportExportValidationException extends RuntimeException {

    public ReportExportValidationException(String message) {
        super(message);
    }
}
