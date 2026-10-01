package com.becommerce.crm.sales.task.application.dto;

import com.becommerce.crm.sales.task.domain.TaskPriority;
import com.becommerce.crm.sales.task.domain.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID companyId,
        UUID contactId,
        UUID opportunityId,
        String title,
        String description,
        UUID assigneeId,
        LocalDateTime dueAt,
        TaskPriority priority,
        TaskStatus status,
        LocalDateTime completedAt,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}