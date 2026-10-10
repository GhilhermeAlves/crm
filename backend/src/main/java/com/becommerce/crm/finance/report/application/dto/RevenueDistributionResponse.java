package com.becommerce.crm.finance.report.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Distribuição das receitas PAID do período (valores em centavos).
 * averageCents/averageNetCents da média por procedimento: net = amount - fee.
 * Profissional com {@code name} "Sem profissional" quando não informado.
 */
public record RevenueDistributionResponse(
        List<RevenueByProcedureItem> byProcedure,
        List<RevenueAverageItem> averageByProcedure,
        List<RevenueByProfessionalItem> byProfessional) {

    public record RevenueByProcedureItem(String period, UUID catalogItemId, String name,
                                         long amountCents) {}

    public record RevenueAverageItem(UUID catalogItemId, String name, long averageCents,
                                     long averageNetCents) {}

    public record RevenueByProfessionalItem(String period, UUID professionalUserId, String name,
                                            long amountCents) {}
}