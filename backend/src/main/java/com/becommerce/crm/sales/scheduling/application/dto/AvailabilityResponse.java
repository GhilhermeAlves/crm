package com.becommerce.crm.sales.scheduling.application.dto;

import java.util.List;

public record AvailabilityResponse(
        String timezone,
        List<AvailabilityRuleDto> rules,
        List<AvailabilityOverrideDto> overrides
) {}
