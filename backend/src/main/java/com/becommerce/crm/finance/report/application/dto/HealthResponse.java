package com.becommerce.crm.finance.report.application.dto;

import java.util.List;

/**
 * Saúde financeira mensal: um item por mês (do mais antigo ao mês atual),
 * incluindo meses sem movimento com zero. Valores em centavos.
 */
public record HealthResponse(List<MonthHealth> months) {

    public record MonthHealth(String month, long incomeCents, long expenseCents, long balanceCents) {}
}