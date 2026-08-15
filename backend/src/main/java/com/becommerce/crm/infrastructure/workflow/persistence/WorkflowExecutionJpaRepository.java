package com.becommerce.crm.infrastructure.workflow.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowExecutionJpaRepository extends JpaRepository<WorkflowExecutionJpaEntity, UUID> {

    List<WorkflowExecutionJpaEntity> findByCompanyIdAndWorkflowIdOrderByCreatedAtDesc(UUID companyId, UUID workflowId);

    List<WorkflowExecutionJpaEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<WorkflowExecutionJpaEntity> findByCompanyIdAndWorkflowIdAndEventId(
            UUID companyId, UUID workflowId, UUID eventId);
}