package com.becommerce.crm.finance.report.application.dto;

/**
 * Indicadores de cabeçalho:
 * revenueThisMonthCents = receitas INCOME PAID no mês corrente (paid_at);
 * attendancesThisWeek = agendamentos COMPLETED na semana corrente (seg-dom);
 * budgetsCreatedThisMonth = oportunidades criadas no mês corrente.
 */
public record HeadlineResponse(
        long revenueThisMonthCents,
        long attendancesThisWeek,
        long budgetsCreatedThisMonth) {}