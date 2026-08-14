package com.becommerce.crm.application.task.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.task.dto.CreateTaskRequest;
import com.becommerce.crm.application.task.dto.TaskResponse;
import com.becommerce.crm.application.task.dto.UpdateTaskRequest;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskStatus;
import com.becommerce.crm.domain.task.exception.TaskNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tasks/Follow-up (Sprint 12). Cada operação isola a empresa ativa no
 * {@link TenantContext} (finally {@code clear()}); os vínculos contact/opportunity
 * são validados como da MESMA empresa (defense-in-depth além do RLS). Auditoria
 * via {@code AuditModule.TASKS}.
 */
@Service
public class TaskService implements TaskUseCase {

    private final TaskRepository taskRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final TenantAuditRecorder auditor;

    public TaskService(TaskRepository taskRepository, ContactRepository contactRepository,
                       OpportunityRepository opportunityRepository, TenantAuditRecorder auditor) {
        this.taskRepository = taskRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public TaskResponse create(UUID companyId, CreateTaskRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            validateOwnedLinks(companyId, request.contactId(), request.opportunityId());
            Task task = Task.create(companyId, request.contactId(), request.opportunityId(),
                    request.title(), request.description(), request.assigneeId(), request.dueAt(),
                    request.priority(), createdBy);
            taskRepository.save(task);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.TASKS, "Task",
                    task.getId().toString(), "Tarefa criada: " + task.getTitle(), createdBy, null);
            return toResponse(task);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(UUID companyId, UUID taskId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, taskId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public TaskResponse update(UUID companyId, UUID taskId, UpdateTaskRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Task task = requireOwned(companyId, taskId);
            validateOwnedLinks(companyId, request.contactId(), request.opportunityId());
            task.update(request.title(), request.description(), request.assigneeId(), request.dueAt(),
                    request.priority(), request.contactId(), request.opportunityId());
            taskRepository.save(task);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.TASKS, "Task",
                    task.getId().toString(), "Tarefa atualizada: " + task.getTitle(), null, null);
            return toResponse(task);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public TaskResponse changeStatus(UUID companyId, UUID taskId, TaskStatus status) {
        try {
            TenantContext.setCompanyId(companyId);
            Task task = requireOwned(companyId, taskId);
            switch (status) {
                case IN_PROGRESS -> task.markInProgress();
                case COMPLETED -> task.complete();
                case CANCELLED -> task.cancel();
                case PENDING -> task.reopen();
            }
            taskRepository.save(task);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.TASKS, "Task",
                    task.getId().toString(), "Tarefa " + status.name().toLowerCase() + ": " + task.getTitle(),
                    null, null);
            return toResponse(task);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID taskId) {
        try {
            TenantContext.setCompanyId(companyId);
            Task task = requireOwned(companyId, taskId);
            taskRepository.delete(task);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.TASKS, "Task",
                    taskId.toString(), "Tarefa excluída: " + task.getTitle(), null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listByCompany(UUID companyId, TaskStatus status) {
        try {
            TenantContext.setCompanyId(companyId);
            List<Task> tasks = status != null
                    ? taskRepository.findByCompanyIdAndStatus(companyId, status)
                    : taskRepository.findByCompanyId(companyId);
            return tasks.stream().map(TaskService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listByOpportunity(UUID companyId, UUID opportunityId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedOpportunity(companyId, opportunityId);
            return taskRepository.findByCompanyIdAndOpportunityId(companyId, opportunityId).stream()
                    .map(TaskService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listDueToday(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            LocalDate today = LocalDate.now();
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
            return taskRepository.findDueToday(companyId, start, end).stream()
                    .map(TaskService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    private void validateOwnedLinks(UUID companyId, UUID contactId, UUID opportunityId) {
        if (contactId != null) {
            var contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> new ContactNotFoundException(contactId));
            if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
                throw new ContactNotFoundException(contactId);
            }
        }
        if (opportunityId != null) {
            requireOwnedOpportunity(companyId, opportunityId);
        }
    }

    private void requireOwnedOpportunity(UUID companyId, UUID opportunityId) {
        var opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
        if (!opportunity.getCompanyId().equals(companyId)) {
            throw new OpportunityNotFoundException(opportunityId);
        }
    }

    private Task requireOwned(UUID companyId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        if (!task.getCompanyId().equals(companyId)) {
            throw new TaskNotFoundException(taskId);
        }
        return task;
    }

    private static TaskResponse toResponse(Task t) {
        return new TaskResponse(t.getId(), t.getCompanyId(), t.getContactId(), t.getOpportunityId(),
                t.getTitle(), t.getDescription(), t.getAssigneeId(), t.getDueAt(), t.getPriority(),
                t.getStatus(), t.getCompletedAt(), t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedAt());
    }
}