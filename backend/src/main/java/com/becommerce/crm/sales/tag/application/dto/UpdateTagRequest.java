package com.becommerce.crm.sales.tag.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
        @Size(max = 120, message = "Nome do marcador deve ter no máximo 120 caracteres")
        String name,
        String color
) {}
