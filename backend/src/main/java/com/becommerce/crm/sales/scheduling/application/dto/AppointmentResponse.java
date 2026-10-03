package com.becommerce.crm.sales.scheduling.application.dto;

import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import com.becommerce.crm.sales.scheduling.domain.LocationKind;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID companyId,
        UUID appointmentTypeId,
        UUID hostId,
        UUID contactId,
        UUID opportunityId,
        String title,
        Instant startAt,
        Instant endAt,
        AppointmentStatus status,
        AppointmentSource source,
        LocationKind locationKind,
        String locationDetail,
        String meetingUrl,
        String notes,
        String cancelReason,
        String publicToken,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
