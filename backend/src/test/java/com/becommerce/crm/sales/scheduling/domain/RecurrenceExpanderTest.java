package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceEnd;
import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceFrequency;
import com.becommerce.crm.sales.scheduling.application.dto.RecurrenceRequest;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceExpanderTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static ZonedDateTime at(int y, int m, int d, int h) {
        return LocalDateTime.of(y, m, d, h, 0).atZone(ZONE);
    }

    private static List<Interval> expand(ZonedDateTime start, RecurrenceRequest r) {
        return RecurrenceExpander.expand(start.toInstant(), start.plusHours(1).toInstant(), r);
    }

    private static LocalDateTime local(Interval i) {
        return i.start().atZone(ZONE).toLocalDateTime();
    }

    @Test
    void nullRecurrenceReturnsOnlyOriginal() {
        assertEquals(1, expand(at(2026, 11, 2, 9), null).size());
    }

    @Test
    void dailyThreeOccurrences() {
        List<Interval> list = expand(at(2026, 11, 2, 9),
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.AFTER_COUNT, null, 3));
        assertEquals(3, list.size());
        assertEquals(LocalDateTime.of(2026, 11, 4, 9, 0), local(list.get(2)));
        assertEquals(java.time.Duration.ofHours(1),
                java.time.Duration.between(list.get(2).start(), list.get(2).end()));
    }

    @Test
    void weeklyEveryTwoWeeks() {
        List<Interval> list = expand(at(2026, 11, 2, 9),
                new RecurrenceRequest(RecurrenceFrequency.WEEKLY, 2, RecurrenceEnd.AFTER_COUNT, null, 3));
        assertEquals(LocalDateTime.of(2026, 11, 16, 9, 0), local(list.get(1)));
        assertEquals(LocalDateTime.of(2026, 11, 30, 9, 0), local(list.get(2)));
    }

    @Test
    void monthlyFromDay31UsesLastDayOfMonth() {
        List<Interval> list = expand(at(2027, 1, 31, 9),
                new RecurrenceRequest(RecurrenceFrequency.MONTHLY, 1, RecurrenceEnd.AFTER_COUNT, null, 3));
        assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0), local(list.get(1)));
        assertEquals(LocalDateTime.of(2027, 3, 31, 9, 0), local(list.get(2)));
    }

    @Test
    void yearly() {
        List<Interval> list = expand(at(2028, 2, 29, 9),
                new RecurrenceRequest(RecurrenceFrequency.YEARLY, 1, RecurrenceEnd.AFTER_COUNT, null, 2));
        assertEquals(LocalDateTime.of(2029, 2, 28, 9, 0), local(list.get(1)));
    }

    @Test
    void onDateIsInclusive() {
        List<Interval> list = expand(at(2026, 11, 2, 9),
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.ON_DATE,
                        LocalDate.of(2026, 11, 5), null));
        assertEquals(4, list.size());
    }

    @Test
    void neverIsLimitedToOneYear() {
        List<Interval> list = expand(at(2026, 11, 2, 9),
                new RecurrenceRequest(RecurrenceFrequency.WEEKLY, 1, RecurrenceEnd.NEVER, null, null));
        assertEquals(53, list.size());
        assertFalse(local(list.get(list.size() - 1)).isAfter(LocalDateTime.of(2027, 11, 2, 9, 0)));
    }

    @Test
    void capsAt366() {
        assertEquals(366, expand(at(2026, 11, 2, 9),
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.NEVER, null, null)).size());
        assertEquals(366, expand(at(2026, 11, 2, 9),
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.ON_DATE,
                        LocalDate.of(2030, 1, 1), null)).size());
    }

    @Test
    void validations() {
        ZonedDateTime s = at(2026, 11, 2, 9);
        assertThrows(SchedulingValidationException.class, () -> expand(s,
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.ON_DATE, LocalDate.of(2026, 11, 1), null)));
        assertThrows(SchedulingValidationException.class, () -> expand(s,
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.ON_DATE, null, null)));
        assertThrows(SchedulingValidationException.class, () -> expand(s,
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.AFTER_COUNT, null, 1)));
        assertThrows(SchedulingValidationException.class, () -> expand(s,
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 1, RecurrenceEnd.AFTER_COUNT, null, 367)));
        assertThrows(SchedulingValidationException.class, () -> expand(s,
                new RecurrenceRequest(RecurrenceFrequency.DAILY, 0, RecurrenceEnd.NEVER, null, null)));
        assertThrows(SchedulingValidationException.class, () -> expand(s,
                new RecurrenceRequest(null, 1, RecurrenceEnd.NEVER, null, null)));
    }
}
