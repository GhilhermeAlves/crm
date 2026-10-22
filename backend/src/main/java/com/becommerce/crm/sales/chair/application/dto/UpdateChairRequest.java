package com.becommerce.crm.sales.chair.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateChairRequest(
        @Size(max = 120, message = "Nome da cadeira deve ter no máximo 120 caracteres")
        String name,
        String color,
        Boolean active,
        Integer sortOrder
) {}
