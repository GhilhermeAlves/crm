package com.becommerce.crm.finance.report.application.dto;

/**
 * Resumo financeiro do período. Sempre em centavos.
 * <ul>
 *   <li>incomeTotalCents / expenseTotalCents: PAID dentro do período (por paid_at);</li>
 *   <li>incomeForecastCents / expenseForecastCents: PENDING com vencimento no período;</li>
 *   <li>balanceCents: incomeTotalCents - expenseTotalCents.</li>
 * </ul>
 * CANCELLED nunca entra nas somas.
 */
public record SummaryResponse(
        long incomeTotalCents,
        long expenseTotalCents,
        long incomeForecastCents,
        long expenseForecastCents,
        long balanceCents) {}