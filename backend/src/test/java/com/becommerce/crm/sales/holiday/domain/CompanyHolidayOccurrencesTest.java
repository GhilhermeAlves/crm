package com.becommerce.crm.sales.holiday.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompanyHolidayOccurrencesTest {

    private final UUID companyId = UUID.randomUUID();

    private CompanyHoliday holiday(LocalDate date, boolean recurring) {
        return CompanyHoliday.create(companyId, date, "Aniversário da clínica", recurring, true);
    }

    @Test
    void oneOffHolidayOnlyInItsDate() {
        CompanyHoliday h = holiday(LocalDate.of(2026, 12, 24), false);

        assertEquals(1, CompanyHoliday.occurrencesBetween(List.of(h),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)).size());
        assertTrue(CompanyHoliday.occurrencesBetween(List.of(h),
                LocalDate.of(2027, 12, 1), LocalDate.of(2027, 12, 31)).isEmpty());
    }

    @Test
    void recurringHolidayRepeatsInLaterYears() {
        CompanyHoliday h = holiday(LocalDate.of(2025, 3, 10), true);

        List<CompanyHoliday> result = CompanyHoliday.occurrencesBetween(List.of(h),
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31));

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2027, 3, 10), result.get(0).getDate());
        assertEquals(h.getId(), result.get(0).getId());
    }

    @Test
    void recurringHolidayDoesNotGoBackBeforeCreationYear() {
        CompanyHoliday h = holiday(LocalDate.of(2026, 3, 10), true);

        assertTrue(CompanyHoliday.occurrencesBetween(List.of(h),
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).isEmpty());
    }

    @Test
    void rangeCrossingNewYearAndSorted() {
        CompanyHoliday christmas = holiday(LocalDate.of(2024, 12, 25), true);
        CompanyHoliday newYear = holiday(LocalDate.of(2024, 1, 2), true);

        List<CompanyHoliday> result = CompanyHoliday.occurrencesBetween(List.of(newYear, christmas),
                LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 10));

        assertEquals(List.of(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 2)),
                result.stream().map(CompanyHoliday::getDate).toList());
    }

    @Test
    void leapDayFallsOnFeb28() {
        CompanyHoliday h = holiday(LocalDate.of(2024, 2, 29), true);

        List<CompanyHoliday> result = CompanyHoliday.occurrencesBetween(List.of(h),
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28));

        assertEquals(LocalDate.of(2027, 2, 28), result.get(0).getDate());
    }
}
