package com.becommerce.crm.sales.activity.application.dto;

import com.becommerce.crm.sales.activity.domain.ActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateActivityRequest(
        @NotNull(message = "Tipo é obrigatório") ActivityType type,
        @NotNull(message = "Assunto é obrigatório")
        @Size(max = 255, message = "Assunto deve ter no máximo 255 caracteres")
        String subject,
        String description,
        LocalDateTime activityAt
) {
}