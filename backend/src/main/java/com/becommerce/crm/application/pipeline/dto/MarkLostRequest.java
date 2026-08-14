package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarkLostRequest(
        @NotBlank(message = "Motivo da perda é obrigatório.")
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres.")
        String lossReason
) {}
