package com.becommerce.crm.sales.scheduling.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilityOverride(UUID id, UUID companyId, UUID userId, LocalDate date,
                                   List<TimeRange> windows) {

    public boolean isUnavailable() {
        return windows == null || windows.isEmpty();
    }
}
