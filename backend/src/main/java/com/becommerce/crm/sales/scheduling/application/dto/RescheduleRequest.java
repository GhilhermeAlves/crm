package com.becommerce.crm.sales.scheduling.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleRequest(
        @NotNull(message = "Nova data de início é obrigatória")
        Instant startAt,
        @NotNull(message = "Nova data de fim é obrigatória")
        Instant endAt,
        Boolean force
) {}
