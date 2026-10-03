package com.becommerce.crm.sales.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityWindow(UUID id, UUID companyId, UUID userId, DayOfWeek weekday,
                                 LocalTime start, LocalTime end) {

    public AvailabilityWindow {
        if (end.isBefore(start) || end.equals(start)) {
            throw new IllegalArgumentException("end deve ser posterior a start");
        }
    }
}
