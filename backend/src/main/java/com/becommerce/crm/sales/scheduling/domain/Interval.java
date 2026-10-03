package com.becommerce.crm.sales.scheduling.domain;

import java.time.Instant;

public record Interval(Instant start, Instant end) {

    public Interval {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end deve ser posterior a start");
        }
    }

    public boolean overlaps(Interval other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
