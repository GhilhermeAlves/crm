package com.becommerce.crm.application.task.dto;

import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;

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