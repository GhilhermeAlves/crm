package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.BudgetRow;

import java.util.List;

/** Orçamentos criados no período: resumo por situação e lista detalhada. */
public record BudgetsDataset(List<StatusSummary> byStatus, List<BudgetRow> rows) {

    /** Situação do orçamento (DRAFT/APPROVED/LOST), com quantidade e valor total. */
    public record StatusSummary(String status, long count, long totalCents) {}

    public long count() {
        return rows.size();
    }

    public long totalCents() {
        return rows.stream().mapToLong(BudgetRow::totalCents).sum();
    }
}
