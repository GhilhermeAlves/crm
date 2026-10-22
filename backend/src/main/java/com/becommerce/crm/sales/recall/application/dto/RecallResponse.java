package com.becommerce.crm.sales.recall.application.dto;

import com.becommerce.crm.sales.recall.domain.RecallStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecallResponse(
        UUID id,
        UUID companyId,
        UUID contactId,
        String phone,
        UUID hostId,
        String tag,
        LocalDate dueDate,
        RecallStatus status,
        UUID appointmentId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        UUID tagId
) {}
