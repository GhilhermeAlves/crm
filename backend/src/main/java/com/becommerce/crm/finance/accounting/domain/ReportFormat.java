package com.becommerce.crm.finance.accounting.domain;

/**
 * Formato de arquivo de um relatório contábil (Etapa 3). Persistido como
 * {@code String} (coluna {@code format}) da tabela
 * {@code accounting_report_requests} (V093).
 */
public enum ReportFormat {
    PDF,
    CSV
}