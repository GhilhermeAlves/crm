package com.becommerce.crm.sales.scheduling.application.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRuleDto(
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime
) {}
