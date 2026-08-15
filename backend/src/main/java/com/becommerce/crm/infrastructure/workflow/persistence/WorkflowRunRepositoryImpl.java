package com.becommerce.crm.infrastructure.workflow.persistence;

import com.becommerce.crm.application.workflow.port.output.WorkflowRunRepository;
import com.becommerce.crm.domain.workflow.WorkflowRun;
import com.becommerce.crm.domain.workflow.WorkflowRunStatus;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de execuções de regra (workflow_runs, Sprint 15). Escrita via SQL
 * nativo para idempotência (chave company/workflow/event); leitura via JPA com
 * filtros + paginação.
 */
@Repository
public class WorkflowRunRepositoryImpl implements WorkflowRunRepository {

    private final WorkflowRunJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public WorkflowRunRepositoryImpl(WorkflowRunJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int insertNew(UUID id, UUID companyId, UUID workflowId, UUID eventId, String eventType,
                         UUID entityId, WorkflowRunStatus status, String conditionsJson, String contextJson) {
        return entityManager.createNativeQuery("""
                INSERT INTO workflow_runs
                    (id, company_id, workflow_id, event_id, event_type, entity_id,
                     status, conditions, context, created_at, updated_at)
                VALUES
                    (:id, :companyId, :workflowId, :eventId, :eventType, :entityId,
                     :status, :conditions, :context, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (company_id, workflow_id, event_id) DO NOTHING
                """)
                .setParameter("id", id)
                .setParameter("companyId", companyId)
                .setParameter("workflowId", workflowId)
                .setParameter("eventId", eventId)
                .setParameter("eventType", eventType)
                .setParameter("entityId", entityId)
                .setParameter("status", status.name())
                .setParameter("conditions", conditionsJson)
                .setParameter("context", contextJson)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, UUID companyId, WorkflowRunStatus status, String resultText) {
        entityManager.createNativeQuery("""
                UPDATE workflow_runs
                SET status = :status, result_text = :result, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND company_id = :companyId
                """)
                .setParameter("id", id)
                .setParameter("companyId", companyId)
                .setParameter("status", status.name())
                .setParameter("result", resultText)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowRun> findById(UUID id, UUID companyId) {
        return jpaRepository.findById(id)
                .filter(e -> e.getCompanyId().equals(companyId))
                .map(WorkflowRunRepositoryImpl::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult findByCompanyAndWorkflow(UUID companyId, UUID workflowId, String status,
                                               String eventType, LocalDateTime from, LocalDateTime to,
                                               int page, int pageSize) {
        var result = jpaRepository.findRuns(companyId, workflowId, status, eventType, from, to,
                PageRequest.of(page, pageSize));
        return toPageResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult findByCompany(UUID companyId, String status, String eventType,
                                    LocalDateTime from, LocalDateTime to, int page, int pageSize) {
        var result = jpaRepository.findCompanyRuns(companyId, status, eventType, from, to,
                PageRequest.of(page, pageSize));
        return toPageResult(result);
    }

    private PageResult toPageResult(org.springframework.data.domain.Page<WorkflowRunJpaEntity> result) {
        List<WorkflowRun> content = result.getContent().stream()
                .map(WorkflowRunRepositoryImpl::toDomain)
                .toList();
        return new PageResult(content, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunSummaryRow> summarizeByCompany(UUID companyId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT r.workflow_id,
                       (SELECT count(*) FROM workflow_runs r2
                        WHERE r2.company_id = :companyId AND r2.workflow_id = r.workflow_id) AS run_count,
                       r.status AS last_status,
                       r.created_at AS last_at,
                       r.event_id AS last_event_id
                FROM workflow_runs r
                WHERE r.company_id = :companyId
                  AND r.created_at = (SELECT max(r3.created_at) FROM workflow_runs r3
                                      WHERE r3.company_id = :companyId AND r3.workflow_id = r.workflow_id)
                ORDER BY r.workflow_id
                """)
                .setParameter("companyId", companyId)
                .getResultList();
        return rows.stream()
                .map(row -> new RunSummaryRow(
                        (UUID) row[0],
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        (java.sql.Timestamp) row[3] != null ? ((java.sql.Timestamp) row[3]).toLocalDateTime() : null,
                        (UUID) row[4]))
                .toList();
    }

    private static WorkflowRun toDomain(WorkflowRunJpaEntity e) {
        return WorkflowRun.reconstitute(e.getId(), e.getCompanyId(), e.getWorkflowId(),
                e.getEventId(), e.getEventType(), e.getEntityId(),
                e.getStatus() != null ? WorkflowRunStatus.valueOf(e.getStatus()) : null,
                e.getConditions(), e.getContext(), e.getResultText(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
