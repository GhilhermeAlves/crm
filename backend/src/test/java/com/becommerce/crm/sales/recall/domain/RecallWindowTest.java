package com.becommerce.crm.sales.recall.domain;

import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallWindowTest {

    private final LocalDate today = LocalDate.of(2026, 10, 21);

    private boolean inWindow(RecallWindow window, LocalDate date) {
        LocalDate from = window.fromInclusive(today);
        LocalDate to = window.toExclusive(today);
        return (from == null || !date.isBefore(from)) && (to == null || date.isBefore(to));
    }

    @Test
    void todayIncludesOnlyToday() {
        RecallWindow window = RecallWindow.TODAY;
        assertTrue(inWindow(window, today));
        assertFalse(inWindow(window, today.minusDays(1)));
        assertFalse(inWindow(window, today.plusDays(1)));
    }

    @Test
    void nextSevenDaysIncludesTodayThroughDaySeven() {
        RecallWindow window = RecallWindow.NEXT_7_DAYS;
        assertTrue(inWindow(window, today));
        assertTrue(inWindow(window, today.plusDays(7)));
        assertFalse(inWindow(window, today.minusDays(1)));
        assertFalse(inWindow(window, today.plusDays(8)));
    }

    @Test
    void nextThirtyDaysIncludesTodayThroughDayThirty() {
        RecallWindow window = RecallWindow.NEXT_30_DAYS;
        assertTrue(inWindow(window, today));
        assertTrue(inWindow(window, today.plusDays(30)));
        assertFalse(inWindow(window, today.plusDays(31)));
    }

    @Test
    void overdueIncludesYesterdayAndExcludesToday() {
        RecallWindow window = RecallWindow.OVERDUE;
        assertNull(window.fromInclusive(today));
        assertEquals(today, window.toExclusive(today));
        assertTrue(inWindow(window, today.minusDays(1)));
        assertFalse(inWindow(window, today));
    }

    @Test
    void allHasNoBounds() {
        assertNull(RecallWindow.ALL.fromInclusive(today));
        assertNull(RecallWindow.ALL.toExclusive(today));
        assertTrue(inWindow(RecallWindow.ALL, today.minusYears(1)));
        assertTrue(inWindow(RecallWindow.ALL, today.plusYears(1)));
    }

    @Test
    void defaultsToNextSevenDaysWhenBlank() {
        assertEquals(RecallWindow.NEXT_7_DAYS, RecallWindow.from(null));
        assertEquals(RecallWindow.NEXT_7_DAYS, RecallWindow.from("  "));
    }

    @Test
    void parsesKnownWindowsCaseInsensitive() {
        assertEquals(RecallWindow.OVERDUE, RecallWindow.from("overdue"));
        assertEquals(RecallWindow.TODAY, RecallWindow.from("TODAY"));
    }

    @Test
    void rejectsUnknownWindow() {
        assertThrows(RecallValidationException.class, () -> RecallWindow.from("LAST_YEAR"));
    }
}
