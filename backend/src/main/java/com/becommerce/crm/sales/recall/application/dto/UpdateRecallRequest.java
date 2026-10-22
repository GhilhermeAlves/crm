package com.becommerce.crm.sales.recall.application.dto;

import com.becommerce.crm.sales.recall.domain.RecallDueUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRecallRequest(
        @Size(max = 20, message = "Celular deve ter no máximo 20 caracteres")
        String phone,
        UUID hostId,
        @Size(max = 120, message = "Marcador deve ter no máximo 120 caracteres")
        String tag,
        @Min(value = 1, message = "Prazo deve ser maior que zero")
        Integer dueInAmount,
        RecallDueUnit dueInUnit,
        UUID tagId
) {}
