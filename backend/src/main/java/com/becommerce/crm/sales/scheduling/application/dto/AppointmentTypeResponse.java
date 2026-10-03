package com.becommerce.crm.sales.scheduling.application.dto;

import com.becommerce.crm.sales.scheduling.domain.AssignmentMode;
import com.becommerce.crm.sales.scheduling.domain.LocationKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppointmentTypeResponse(
        UUID id,
        UUID companyId,
        String name,
        String slug,
        String description,
        int durationMinutes,
        int bufferBeforeMinutes,
        int bufferAfterMinutes,
        int minNoticeHours,
        int maxDaysAhead,
        int slotIntervalMinutes,
        String color,
        LocationKind locationKind,
        String locationDetail,
        AssignmentMode assignmentMode,
        boolean publicBookingEnabled,
        boolean active,
        List<UUID> hostIds,
        Instant createdAt,
        Instant updatedAt
) {}
