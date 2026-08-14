package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateOpportunityRequest(
        @Size(min = 1, max = 200, message = "Título deve ter entre 1 e 200 caracteres.")
        String title,
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero.")
        BigDecimal value,
        UUID assignedTo,
        LocalDateTime expectedCloseDate,
        @Size(max = 1000, message = "Notas devem ter no máximo 1000 caracteres.")
        String notes
) {}
