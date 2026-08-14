package com.becommerce.crm.application.task.port.output;

import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(UUID id);

    List<Task> findByCompanyId(UUID companyId);

    List<Task> findByCompanyIdAndStatus(UUID companyId, TaskStatus status);

    List<Task> findByCompanyIdAndAssigneeId(UUID companyId, UUID assigneeId);

    List<Task> findByCompanyIdAndOpportunityId(UUID companyId, UUID opportunityId);

    /** Tarefas associadas a um contato (qualquer estado). */
    List<Task> findByContactId(UUID contactId);

    List<Task> findDueToday(UUID companyId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    long countPendingByCompanyId(UUID companyId);

    void delete(Task task);
}