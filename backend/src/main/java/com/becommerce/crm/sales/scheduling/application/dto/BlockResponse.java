package com.becommerce.crm.sales.scheduling.application.dto;

import com.becommerce.crm.sales.scheduling.domain.BlockSource;

import java.time.Instant;
import java.util.UUID;

public record BlockResponse(
        UUID id,
        UUID companyId,
        UUID hostId,
        Instant startAt,
        Instant endAt,
        String reason,
        BlockSource source,
        UUID createdBy,
        Instant createdAt
) {}
