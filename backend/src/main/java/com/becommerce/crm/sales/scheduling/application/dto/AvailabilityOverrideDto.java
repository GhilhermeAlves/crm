package com.becommerce.crm.sales.scheduling.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityOverrideDto(
        LocalDate date,
        List<TimeSlotDto> windows
) {
    public record TimeSlotDto(LocalTime start, LocalTime end) {}
}
