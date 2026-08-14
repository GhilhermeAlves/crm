package com.becommerce.crm.infrastructure.task.persistence;

import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskJpaRepository jpaRepository;

    public TaskRepositoryImpl(TaskJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Task save(Task task) {
        return toDomain(jpaRepository.save(toEntity(task)));
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return jpaRepository.findById(id).map(TaskRepositoryImpl::toDomain);
    }

    @Override
    public List<Task> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(TaskRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Task> findByCompanyIdAndStatus(UUID companyId, TaskStatus status) {
        return jpaRepository.findByCompanyIdAndStatus(companyId, status == null ? null : status.name())
                .stream().map(TaskRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Task> findByCompanyIdAndAssigneeId(UUID companyId, UUID assigneeId) {
        return jpaRepository.findByCompanyIdAndAssigneeId(companyId, assigneeId).stream()
                .map(TaskRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Task> findByCompanyIdAndOpportunityId(UUID companyId, UUID opportunityId) {
        return jpaRepository.findByCompanyIdAndOpportunityId(companyId, opportunityId).stream()
                .map(TaskRepositoryImpl::toDomain).toList();
    }

    @Override
    public List<Task> findDueToday(UUID companyId, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCompanyIdAndDueAtBetween(companyId, start, end).stream()
                .map(TaskRepositoryImpl::toDomain).toList();
    }

    @Override
    public long countPendingByCompanyId(UUID companyId) {
        return jpaRepository.countByCompanyIdAndStatus(companyId, TaskStatus.PENDING.name());
    }

    @Override
    public void delete(Task task) {
        jpaRepository.deleteById(task.getId());
    }

    private static TaskJpaEntity toEntity(Task t) {
        TaskJpaEntity e = new TaskJpaEntity();
        e.setId(t.getId());
        e.setCompanyId(t.getCompanyId());
        e.setContactId(t.getContactId());
        e.setOpportunityId(t.getOpportunityId());
        e.setTitle(t.getTitle());
        e.setDescription(t.getDescription());
        e.setAssigneeId(t.getAssigneeId());
        e.setDueAt(t.getDueAt());
        e.setPriority(t.getPriority() != null ? t.getPriority().name() : null);
        e.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        e.setCompletedAt(t.getCompletedAt());
        e.setCreatedBy(t.getCreatedBy());
        e.setCreatedAt(t.getCreatedAt());
        e.setUpdatedAt(t.getUpdatedAt());
        return e;
    }

    private static Task toDomain(TaskJpaEntity e) {
        return Task.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getOpportunityId(),
                e.getTitle(), e.getDescription(), e.getAssigneeId(), e.getDueAt(),
                e.getPriority() != null ? TaskPriority.valueOf(e.getPriority()) : null,
                e.getStatus() != null ? TaskStatus.valueOf(e.getStatus()) : null,
                e.getCompletedAt(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}