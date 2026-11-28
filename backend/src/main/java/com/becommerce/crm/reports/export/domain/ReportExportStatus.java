package com.becommerce.crm.reports.export.domain;

/**
 * Ciclo de vida de uma exportação: PENDING → READY | FAILED; READY → EXPIRED
 * quando o arquivo passa da retenção de 7 dias e é expurgado do storage;
 * PENDING → CANCELED quando o autor cancela (terminal).
 */
public enum ReportExportStatus {
    PENDING,
    READY,
    FAILED,
    EXPIRED,
    CANCELED
}
