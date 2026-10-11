package com.becommerce.crm.finance.report.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Agrupamento temporal dos relatórios financeiros (Etapa 2, somente leitura).
 * Os períodos são truncados ao início (dia / segunda-feira da semana / primeiro
 * dia do mês), alinhados ao {@code date_trunc} usado nas consultas nativas.
 */
public enum ReportGranularity {

    DAY,
    WEEK,
    MONTH;

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Nome passado ao {@code date_trunc} do PostgreSQL (whitelist do próprio enum). */
    public String trunc() {
        return switch (this) {
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
        };
    }

    /** Rótulo do período: {@code yyyy-MM} em MONTH, caso contrário {@code yyyy-MM-dd}. */
    public String label(LocalDate periodStart) {
        return this == MONTH ? MONTH_LABEL.format(periodStart) : DAY_LABEL.format(periodStart);
    }

    /** Início do período que contém {@code date}, consistente com o {@code date_trunc}. */
    public LocalDate truncate(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case WEEK -> date.with(DayOfWeek.MONDAY);
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    /** Próximo período (usado para montar a série temporal completa, incluindo zeros). */
    public LocalDate next(LocalDate periodStart) {
        return switch (this) {
            case DAY -> periodStart.plusDays(1);
            case WEEK -> periodStart.plusWeeks(1);
            case MONTH -> periodStart.plusMonths(1);
        };
    }
}