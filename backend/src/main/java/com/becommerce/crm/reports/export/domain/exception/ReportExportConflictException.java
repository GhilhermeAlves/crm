package com.becommerce.crm.reports.export.domain.exception;

/** Operação incompatível com a situação atual da exportação (ex.: cancelar uma já pronta) — 409. */
public class ReportExportConflictException extends RuntimeException {

    public ReportExportConflictException(String message) {
        super(message);
    }
}
