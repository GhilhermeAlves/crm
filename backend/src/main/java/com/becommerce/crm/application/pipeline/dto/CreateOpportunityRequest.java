package com.becommerce.crm.application.pipeline.dto;

import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateOpportunityRequest(
        @NotBlank(message = "Título da oportunidade é obrigatório.")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres.")
        String title,
        @NotNull(message = "Valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero.")
        BigDecimal value,
        @NotNull(message = "Contato é obrigatório.")
        UUID contactId,
        UUID assignedTo,
        LocalDateTime expectedCloseDate,
        @Size(max = 1000, message = "Notas devem ter no máximo 1000 caracteres.")
        String notes
) {}
