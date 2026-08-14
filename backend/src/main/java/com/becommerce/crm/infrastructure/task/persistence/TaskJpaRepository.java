package com.becommerce.crm.infrastructure.task.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {

    List<TaskJpaEntity> findByCompanyId(UUID companyId);

    List<TaskJpaEntity> findByCompanyIdAndStatus(UUID companyId, String status);

    List<TaskJpaEntity> findByCompanyIdAndAssigneeId(UUID companyId, UUID assigneeId);

    List<TaskJpaEntity> findByCompanyIdAndOpportunityId(UUID companyId, UUID opportunityId);

    List<TaskJpaEntity> findByCompanyIdAndDueAtBetween(UUID companyId, LocalDateTime start, LocalDateTime end);

    long countByCompanyIdAndStatus(UUID companyId, String status);
}