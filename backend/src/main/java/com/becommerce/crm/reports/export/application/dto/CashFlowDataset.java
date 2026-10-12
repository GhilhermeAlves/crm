package com.becommerce.crm.reports.export.application.dto;

import java.time.LocalDate;
import java.util.List;

/** Fluxo de caixa realizado: entradas/saídas pagas por dia e saldo acumulado do período. */
public record CashFlowDataset(List<CashFlowDay> days, long incomeCents, long expenseCents) {

    public long balanceCents() {
        return incomeCents - expenseCents;
    }

    public record CashFlowDay(LocalDate date, long incomeCents, long expenseCents, long cumulativeCents) {
        public long balanceCents() {
            return incomeCents - expenseCents;
        }
    }
}
