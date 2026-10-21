package com.becommerce.crm.sales.recall.application.dto;

import com.becommerce.crm.sales.recall.domain.RecallDueUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRecallRequest(
        @NotNull(message = "Paciente é obrigatório")
        UUID contactId,
        @NotBlank(message = "Celular é obrigatório")
        @Size(max = 20, message = "Celular deve ter no máximo 20 caracteres")
        String phone,
        @NotNull(message = "Dentista é obrigatório")
        UUID hostId,
        @Size(max = 120, message = "Marcador deve ter no máximo 120 caracteres")
        String tag,
        @NotNull(message = "Prazo é obrigatório")
        @Min(value = 1, message = "Prazo deve ser maior que zero")
        Integer dueInAmount,
        @NotNull(message = "Unidade do prazo é obrigatória")
        RecallDueUnit dueInUnit
) {}
