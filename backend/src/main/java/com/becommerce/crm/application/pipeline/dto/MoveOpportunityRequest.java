package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MoveOpportunityRequest(
        @NotNull(message = "Direção é obrigatória (ADVANCE ou REGRESS).")
        MoveDirection direction,
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres.")
        String note
) {}
