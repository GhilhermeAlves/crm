package com.becommerce.crm.sales.task.application.dto;

import com.becommerce.crm.sales.task.domain.TaskPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTaskRequest(
        @NotNull(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String title,
        String description,
        UUID assigneeId,
        LocalDateTime dueAt,
        TaskPriority priority,
        UUID contactId,
        UUID opportunityId
) {
}