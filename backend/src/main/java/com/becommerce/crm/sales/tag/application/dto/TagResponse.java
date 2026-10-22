package com.becommerce.crm.sales.tag.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TagResponse(
        UUID id,
        UUID companyId,
        String name,
        String color,
        Instant createdAt,
        Instant updatedAt
) {}
