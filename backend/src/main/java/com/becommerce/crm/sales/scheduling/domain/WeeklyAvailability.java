package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.shared.calendar.BrazilianHolidays;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeeklyAvailability {

    private final List<AvailabilityWindow> windows;
    private final List<AvailabilityOverride> overrides;
    private final ZoneId timezone;

    public WeeklyAvailability(List<AvailabilityWindow> windows,
                              List<AvailabilityOverride> overrides,
                              ZoneId timezone) {
        this.windows = windows;
        this.overrides = overrides;
        this.timezone = timezone;
    }

    public List<TimeRange> getWindowsForDate(LocalDate date) {
        for (AvailabilityOverride override : overrides) {
            if (override.date().equals(date)) {
                return override.isUnavailable() ? List.of() : override.windows();
            }
        }
        // Feriado nacional fecha a agenda; para atender, cadastre uma exceção na data.
        if (BrazilianHolidays.isHoliday(date)) {
            return List.of();
        }
        DayOfWeek dow = date.getDayOfWeek();
        return windows.stream()
                .filter(w -> w.weekday() == dow)
                .map(w -> new TimeRange(w.start(), w.end()))
                .toList();
    }

    public Map<DayOfWeek, List<TimeRange>> getWeeklyWindows() {
        return windows.stream()
                .collect(Collectors.groupingBy(AvailabilityWindow::weekday,
                        Collectors.mapping(w -> new TimeRange(w.start(), w.end()), Collectors.toList())));
    }

    public ZoneId getTimezone() { return timezone; }
    public List<AvailabilityWindow> getWindows() { return windows; }
    public List<AvailabilityOverride> getOverrides() { return overrides; }
}
