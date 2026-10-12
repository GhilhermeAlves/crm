package com.becommerce.crm.reports.export.domain;

/** Ciclo de vida de uma exportação: PENDING → READY | FAILED (terminais). */
public enum ReportExportStatus {
    PENDING,
    READY,
    FAILED
}
