package com.becommerce.crm.finance.accounting.domain;

/**
 * Tipo de relatório contábil (Etapa 3). Persistido como {@code String}
 * (coluna {@code type}) da tabela {@code accounting_report_requests} (V093).
 */
public enum AccountingReportType {
    MONTHLY_ACCOUNTING,
    STATEMENT,
    TRANSACTIONS
}