package com.becommerce.crm.sales.scheduling.domain;

import java.time.LocalTime;

public record TimeRange(LocalTime start, LocalTime end) {

    public TimeRange {
        if (end.isBefore(start) || end.equals(start)) {
            throw new IllegalArgumentException("end deve ser posterior a start");
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
