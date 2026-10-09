package com.becommerce.crm.shared.calendar;

import com.becommerce.crm.sales.scheduling.domain.AvailabilityOverride;
import com.becommerce.crm.sales.scheduling.domain.AvailabilityWindow;
import com.becommerce.crm.sales.scheduling.domain.TimeRange;
import com.becommerce.crm.sales.scheduling.domain.WeeklyAvailability;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BrazilianHolidaysTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    void movableHolidays_followEaster() {
        assertEquals(LocalDate.of(2026, 4, 5), BrazilianHolidays.easterSunday(2026));
        assertEquals("Carnaval", BrazilianHolidays.on(LocalDate.of(2026, 2, 17)).orElseThrow().name());
        assertEquals("Sexta-feira Santa", BrazilianHolidays.on(LocalDate.of(2026, 4, 3)).orElseThrow().name());
        assertEquals("Corpus Christi", BrazilianHolidays.on(LocalDate.of(2026, 6, 4)).orElseThrow().name());
        assertEquals(LocalDate.of(2027, 3, 28), BrazilianHolidays.easterSunday(2027));
    }

    @Test
    void between_spansYearsInOrder() {
        List<BrazilianHolidays.Holiday> h = BrazilianHolidays.between(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5));
        assertEquals(List.of(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 1)),
                h.stream().map(BrazilianHolidays.Holiday::date).toList());
    }

    @Test
    void weeklyAvailability_closesOnHoliday_unlessThereIsAnOverride() {
        UUID company = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        LocalDate aparecida = LocalDate.of(2026, 10, 12); // segunda-feira
        List<AvailabilityWindow> mondays = List.of(new AvailabilityWindow(UUID.randomUUID(), company, user,
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

        assertTrue(new WeeklyAvailability(mondays, List.of(), ZONE).getWindowsForDate(aparecida).isEmpty());
        assertFalse(new WeeklyAvailability(mondays, List.of(), ZONE)
                .getWindowsForDate(aparecida.plusWeeks(1)).isEmpty(), "segunda comum continua aberta");

        List<TimeRange> opened = List.of(new TimeRange(LocalTime.of(8, 0), LocalTime.of(10, 0)));
        AvailabilityOverride override = new AvailabilityOverride(UUID.randomUUID(), company, user, aparecida, opened);
        assertEquals(opened, new WeeklyAvailability(mondays, List.of(override), ZONE).getWindowsForDate(aparecida));
    }
}
