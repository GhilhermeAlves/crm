package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStageRequest(
        @NotBlank(message = "Nome do estágio é obrigatório.")
        @Size(max = 100, message = "Nome do estágio deve ter no máximo 100 caracteres.")
        String name,
        @Size(max = 7, message = "Cor deve ter no máximo 7 caracteres (ex.: #RRGGBB).")
        String color,
        @Min(value = 0, message = "Probabilidade deve ser entre 0 e 100.")
        @Max(value = 100, message = "Probabilidade deve ser entre 0 e 100.")
        Integer probability
) {}
