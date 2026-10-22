package com.becommerce.crm.sales.holiday.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HolidayResponse(
        UUID id,
        UUID companyId,
        LocalDate date,
        String name,
        boolean recurringYearly,
        boolean blocksSchedule,
        Instant createdAt,
        Instant updatedAt
) {}
