package com.becommerce.crm.application.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Períodos e comparação temporal do analytics (Sprint 19). */
class AnalyticsPeriodTest {

    @Test
    void explicitRangeIsInclusiveStartExclusiveEnd() {
        var p = AnalyticsPeriod.resolve(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7), null);
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), p.from());
        assertEquals(LocalDateTime.of(2026, 8, 8, 0, 0), p.to());
    }

    @Test
    void previousPeriodHasSameDurationImmediatelyBefore() {
        var p = AnalyticsPeriod.resolve(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null);
        var prev = p.previous();
        assertEquals(p.from(), prev.to());
        assertEquals(p.from().minusDays(31), prev.from());
    }

    @Test
    void invalidRangeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AnalyticsPeriod.resolve(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1), null));
    }

    @Test
    void timezoneChangesDayBoundary() {
        var utc = AnalyticsPeriod.resolve(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 26), "UTC");
        var saoPaulo = AnalyticsPeriod.resolve(
                LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 26), "America/Sao_Paulo");
        // o dia começa em 00:00 no timezone informado (início inclusivo)
        assertEquals(LocalDateTime.of(2026, 8, 25, 0, 0), saoPaulo.from());
        assertEquals(LocalDateTime.of(2026, 8, 27, 0, 0), saoPaulo.to());
        assertEquals(LocalDateTime.of(2026, 8, 27, 0, 0), utc.to());
    }

    @Test
    void defaultPeriodSpansThirtyDays() {
        var p = AnalyticsPeriod.resolve(null, null, null);
        assertEquals(java.time.Duration.ofDays(30), java.time.Duration.between(p.from(), p.to()));
    }
}
