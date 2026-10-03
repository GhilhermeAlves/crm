package com.becommerce.crm.sales.scheduling.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateBlockRequest(
        @NotNull(message = "Responsável é obrigatório")
        UUID hostId,
        @NotNull(message = "Data de início é obrigatória")
        Instant startAt,
        @NotNull(message = "Data de fim é obrigatória")
        Instant endAt,
        @Size(max = 200, message = "Motivo deve ter no máximo 200 caracteres")
        String reason
) {}
