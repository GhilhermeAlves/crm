package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePipelineRequest(
        @NotBlank(message = "Nome do pipeline é obrigatório.")
        @Size(max = 100, message = "Nome do pipeline deve ter no máximo 100 caracteres.")
        String name,
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String description
) {}
