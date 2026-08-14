package com.becommerce.crm.domain.task;

import com.becommerce.crm.domain.task.exception.TaskValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Task (Sprint 12) — ação a executar (follow-up, ligar, enviar proposta, agendar
 * reunião...). Pertence a uma empresa (company_id, RLS FORCE). Relacionamento com
 * contact/opportunity é opcional e nullable por design — não acopla a um único
 * tipo de entidade e permite futura associação a Company/Activity sem migração
 * de modelo (Sprint 12: preparação arquitetural, sem engine).
 *
 * <p>Corresponde à tabela {@code tasks} (V039).</p>
 */
public class Task {

    private final UUID id;
    private final UUID companyId;
    private UUID contactId;
    private UUID opportunityId;
    private String title;
    private String description;
    private UUID assigneeId;
    private LocalDateTime dueAt;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDateTime completedAt;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Task(UUID id, UUID companyId, UUID contactId, UUID opportunityId, String title,
                 String description, UUID assigneeId, LocalDateTime dueAt, TaskPriority priority,
                 TaskStatus status, LocalDateTime completedAt, UUID createdBy,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.opportunityId = opportunityId;
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.dueAt = dueAt;
        this.priority = priority;
        this.status = status;
        this.completedAt = completedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Task create(UUID companyId, UUID contactId, UUID opportunityId, String title,
                              String description, UUID assigneeId, LocalDateTime dueAt,
                              TaskPriority priority, UUID createdBy) {
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("O título da tarefa é obrigatório.");
        }
        String normalized = title.trim();
        if (normalized.length() > 200) {
            throw new TaskValidationException("O título deve ter no máximo 200 caracteres.");
        }
        LocalDateTime now = LocalDateTime.now();
        TaskPriority pr = priority != null ? priority : TaskPriority.MEDIUM;
        return new Task(UUID.randomUUID(), companyId, contactId, opportunityId, normalized,
                description, assigneeId, dueAt, pr, TaskStatus.PENDING, null, createdBy, now, now);
    }

    public static Task reconstitute(UUID id, UUID companyId, UUID contactId, UUID opportunityId,
                                    String title, String description, UUID assigneeId, LocalDateTime dueAt,
                                    TaskPriority priority, TaskStatus status, LocalDateTime completedAt,
                                    UUID createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Task(id, companyId, contactId, opportunityId, title, description, assigneeId,
                dueAt, priority, status, completedAt, createdBy, createdAt, updatedAt);
    }

    public void update(String title, String description, UUID assigneeId, LocalDateTime dueAt,
                       TaskPriority priority, UUID contactId, UUID opportunityId) {
        if (status == TaskStatus.COMPLETED) {
            throw new TaskValidationException("Tarefa concluída não pode ser alterada.");
        }
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        this.description = description;
        this.assigneeId = assigneeId;
        this.dueAt = dueAt;
        if (priority != null) {
            this.priority = priority;
        }
        this.contactId = contactId;
        this.opportunityId = opportunityId;
        touch();
    }

    public void markInProgress() {
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            throw new TaskValidationException("Tarefa " + status + " não pode ser reiniciada.");
        }
        this.status = TaskStatus.IN_PROGRESS;
        touch();
    }

    public void complete() {
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            throw new TaskValidationException("Tarefa " + status + " não pode ser concluída.");
        }
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        touch();
    }

    public void cancel() {
        if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
            throw new TaskValidationException("Tarefa " + status + " não pode ser cancelada.");
        }
        this.status = TaskStatus.CANCELLED;
        touch();
    }

    public void reopen() {
        if (status != TaskStatus.COMPLETED && status != TaskStatus.CANCELLED) {
            return;
        }
        this.status = TaskStatus.PENDING;
        this.completedAt = null;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getOpportunityId() { return opportunityId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getAssigneeId() { return assigneeId; }
    public LocalDateTime getDueAt() { return dueAt; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}