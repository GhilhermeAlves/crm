package com.becommerce.crm.infrastructure.workflow.persistence;

import com.becommerce.crm.application.workflow.port.output.WorkflowExecutionRepository;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ExecutionStatus;
import com.becommerce.crm.domain.workflow.WorkflowExecution;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistência de execuções de workflow. A escrita usa SQL nativo para garantir
 * a idempotência (Item 6): {@code INSERT ... ON CONFLICT (company_id,
 * workflow_action_id, event_id) DO NOTHING}. A leitura (histórico) usa JPA.
 */
@Repository
public class WorkflowExecutionRepositoryImpl implements WorkflowExecutionRepository {

    private final WorkflowExecutionJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public WorkflowExecutionRepositoryImpl(WorkflowExecutionJpaRepository jpaRepository,
                                           EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int insertNew(UUID id, UUID companyId, UUID workflowId, UUID workflowActionId, UUID eventId,
                         String eventType, UUID entityId, ActionType actionType) {
        return entityManager.createNativeQuery("""
                INSERT INTO workflow_executions
                    (id, company_id, workflow_id, workflow_action_id, event_id, event_type,
                     entity_id, action_type, status, created_at, updated_at)
                VALUES
                    (:id, :companyId, :workflowId, :workflowActionId, :eventId, :eventType,
                     :entityId, :actionType, 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (company_id, workflow_action_id, event_id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("companyId", companyId)
                .setParameter("workflowId", workflowId)
                .setParameter("workflowActionId", workflowActionId)
                .setParameter("eventId", eventId)
                .setParameter("eventType", eventType)
                .setParameter("entityId", entityId)
                .setParameter("actionType", actionType.name())
                .executeUpdate();
    }

    @Override
    @Transactional
    public void updateResult(UUID id, UUID companyId, ExecutionStatus status, String resultText, String errorMessage) {
        entityManager.createNativeQuery("""
                UPDATE workflow_executions
                SET status = :status, result_text = :result, error_message = :error, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND company_id = :companyId
                """)
                .setParameter("id", id)
                .setParameter("companyId", companyId)
                .setParameter("status", status.name())
                .setParameter("result", resultText)
                .setParameter("error", errorMessage)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecution> findByCompanyIdAndWorkflowId(UUID companyId, UUID workflowId) {
        return jpaRepository.findByCompanyIdAndWorkflowIdOrderByCreatedAtDesc(companyId, workflowId).stream()
                .map(WorkflowExecutionRepositoryImpl::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecution> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(WorkflowExecutionRepositoryImpl::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecution> findByCompanyIdAndWorkflowIdAndEventId(UUID companyId, UUID workflowId, UUID eventId) {
        return jpaRepository.findByCompanyIdAndWorkflowIdAndEventId(companyId, workflowId, eventId).stream()
                .map(WorkflowExecutionRepositoryImpl::toDomain).toList();
    }

    private static WorkflowExecution toDomain(WorkflowExecutionJpaEntity e) {
        return WorkflowExecution.reconstitute(e.getId(), e.getCompanyId(), e.getWorkflowId(),
                e.getWorkflowActionId(), e.getEventId(), e.getEventType(), e.getEntityId(),
                e.getActionType() != null ? ActionType.valueOf(e.getActionType()) : null,
                e.getStatus() != null ? ExecutionStatus.valueOf(e.getStatus()) : null,
                e.getResultText(), e.getErrorMessage(), e.getCreatedAt(), e.getUpdatedAt());
    }
}