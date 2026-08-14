package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.Size;

public record UpdatePipelineRequest(
        @Size(min = 1, max = 100, message = "Nome do pipeline deve ter entre 1 e 100 caracteres.")
        String name,
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String description
) {}
