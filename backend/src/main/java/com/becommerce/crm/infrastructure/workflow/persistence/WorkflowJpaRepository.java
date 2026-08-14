package com.becommerce.crm.infrastructure.workflow.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowJpaRepository extends JpaRepository<WorkflowJpaEntity, UUID> {

    List<WorkflowJpaEntity> findByCompanyId(UUID companyId);

    List<WorkflowJpaEntity> findByCompanyIdAndTriggerAndActive(UUID companyId, String trigger, boolean active);
}