package com.becommerce.crm.finance.accounting.application.dto;

import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;

import java.util.List;

/**
 * Conjunto de dados brutos de um relatório contábil (Etapa 3). Apenas o slot
 * correspondente ao {@code AccountingReportType} é populado.
 */
public record AccountingReportDataset(
        MonthlyAccountingData monthly,
        List<AccountingReportGateway.StatementRow> statementRows,
        List<AccountingReportGateway.TransactionDetailRow> transactionRows
) {
}