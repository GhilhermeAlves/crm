package com.becommerce.crm.sales.recall.domain;

import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecallDueDateTest {

    private final LocalDate today = LocalDate.of(2026, 10, 21);

    @Test
    void calculatesDueDateInDays() {
        assertEquals(LocalDate.of(2026, 10, 24), Recall.dueDateFrom(today, 3, RecallDueUnit.DAYS));
    }

    @Test
    void calculatesDueDateInWeeks() {
        assertEquals(LocalDate.of(2026, 11, 4), Recall.dueDateFrom(today, 2, RecallDueUnit.WEEKS));
    }

    @Test
    void calculatesDueDateInMonths() {
        assertEquals(LocalDate.of(2026, 12, 21), Recall.dueDateFrom(today, 2, RecallDueUnit.MONTHS));
    }

    @Test
    void calculatesDueDateInYears() {
        assertEquals(LocalDate.of(2028, 10, 21), Recall.dueDateFrom(today, 2, RecallDueUnit.YEARS));
    }

    @Test
    void oneDayFromTodayIsTomorrow() {
        assertEquals(today.plusDays(1), Recall.dueDateFrom(today, 1, RecallDueUnit.DAYS));
    }

    @Test
    void rejectsAmountBelowOne() {
        assertThrows(RecallValidationException.class,
                () -> Recall.dueDateFrom(today, 0, RecallDueUnit.DAYS));
    }

    @Test
    void rejectsMissingUnit() {
        assertThrows(RecallValidationException.class,
                () -> Recall.dueDateFrom(today, 1, null));
    }

    @Test
    void rejectsMissingToday() {
        assertThrows(RecallValidationException.class,
                () -> Recall.dueDateFrom(null, 1, RecallDueUnit.DAYS));
    }
}
