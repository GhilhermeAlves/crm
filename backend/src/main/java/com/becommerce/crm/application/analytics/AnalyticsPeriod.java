package com.becommerce.crm.application.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Período de análise (Sprint 19). Datas interpretadas no timezone informado
 * (default America/Sao_Paulo): início inclusivo (00:00) e fim EXCLUSIVO
 * (00:00 do dia seguinte). O período anterior tem a mesma duração,
 * imediatamente anterior ao início — base da comparação temporal.
 */
public record AnalyticsPeriod(LocalDateTime from, LocalDateTime to) {

    private static final ZoneId DEFAULT_TZ = ZoneId.of("America/Sao_Paulo");

    public static AnalyticsPeriod resolve(LocalDate fromDate, LocalDate toDate, String timezone) {
        ZoneId tz = timezone != null && !timezone.isBlank()
                ? ZoneId.of(timezone)
                : DEFAULT_TZ;
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(tz).minusDays(29);
        LocalDate endExclusive = toDate != null ? toDate.plusDays(1) : LocalDate.now(tz).plusDays(1);
        if (!start.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Período inválido: 'from' deve ser anterior a 'to'.");
        }
        return new AnalyticsPeriod(start.atStartOfDay(tz).toLocalDateTime(),
                endExclusive.atStartOfDay(tz).toLocalDateTime());
    }

    /** Período anterior com a mesma duração (comparação temporal). */
    public AnalyticsPeriod previous() {
        var duration = java.time.Duration.between(from, to);
        return new AnalyticsPeriod(from.minus(duration), from);
    }
}
