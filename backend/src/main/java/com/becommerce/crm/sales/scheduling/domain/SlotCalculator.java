package com.becommerce.crm.sales.scheduling.domain;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SlotCalculator {

    private SlotCalculator() {}

    public static List<Interval> freeSlots(LocalDate date, int durationMinutes,
                                           int slotIntervalMinutes, int bufferBeforeMinutes,
                                           int bufferAfterMinutes, int minNoticeHours,
                                           int maxDaysAhead, ZoneId hostZone,
                                           List<TimeRange> availabilityWindows,
                                           List<Interval> busyIntervals) {
        Instant now = Instant.now();
        LocalDate today = now.atZone(hostZone).toLocalDate();

        if (date.isBefore(today)) {
            return List.of();
        }
        if (maxDaysAhead > 0 && date.isAfter(today.plusDays(maxDaysAhead))) {
            return List.of();
        }

        Instant minNoticeThreshold = now.plus(Duration.ofHours(minNoticeHours));

        List<Interval> slots = new ArrayList<>();

        for (TimeRange window : availabilityWindows) {
            Instant windowStart = date.atTime(window.start()).atZone(hostZone).toInstant();
            Instant windowEnd = date.atTime(window.end()).atZone(hostZone).toInstant();

            Instant cursor = windowStart;
            while (true) {
                Instant slotStart = cursor;
                Instant slotEnd = slotStart.plus(Duration.ofMinutes(durationMinutes));

                if (slotEnd.isAfter(windowEnd)) {
                    break;
                }

                if (slotStart.isBefore(minNoticeThreshold)) {
                    cursor = cursor.plus(Duration.ofMinutes(slotIntervalMinutes));
                    continue;
                }

                Instant bufferedStart = slotStart.minus(Duration.ofMinutes(bufferBeforeMinutes));
                Instant bufferedEnd = slotEnd.plus(Duration.ofMinutes(bufferAfterMinutes));
                Interval buffered = new Interval(bufferedStart, bufferedEnd);

                boolean conflict = busyIntervals.stream().anyMatch(busy -> busy.overlaps(buffered));
                if (!conflict) {
                    slots.add(new Interval(slotStart, slotEnd));
                }

                cursor = cursor.plus(Duration.ofMinutes(slotIntervalMinutes));
            }
        }

        slots.sort(Comparator.comparing(Interval::start));
        return slots;
    }
}
