package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.TransactionRow;

import java.util.List;

/** Extrato de lançamentos com vencimento no período (sem cancelados). */
public record TransactionsDataset(List<TransactionRow> rows, long incomeCents, long expenseCents) {

    public long balanceCents() {
        return incomeCents - expenseCents;
    }
}
