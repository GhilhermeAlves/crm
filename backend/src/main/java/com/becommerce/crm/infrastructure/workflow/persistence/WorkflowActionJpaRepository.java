package com.becommerce.crm.infrastructure.workflow.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowActionJpaRepository extends JpaRepository<WorkflowActionJpaEntity, UUID> {

    List<WorkflowActionJpaEntity> findByWorkflowId(UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}