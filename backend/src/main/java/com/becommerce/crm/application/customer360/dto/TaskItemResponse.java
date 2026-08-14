package com.becommerce.crm.application.customer360.dto;

import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Tarefa no contexto do contato (Customer 360). */
public record TaskItemResponse(
        UUID id,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueAt,
        UUID assigneeId,
        LocalDateTime completedAt,
        boolean overdue
) {}