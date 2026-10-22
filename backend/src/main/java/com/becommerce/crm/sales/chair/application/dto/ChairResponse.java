package com.becommerce.crm.sales.chair.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ChairResponse(
        UUID id,
        UUID companyId,
        String name,
        String color,
        boolean active,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {}
