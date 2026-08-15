package com.becommerce.crm.application.workflow.port.output;

import com.becommerce.crm.domain.workflow.WorkflowRun;
import com.becommerce.crm.domain.workflow.WorkflowRunStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRunRepository {

    /** Insere idempotentemente (chave company/workflow/event). 1 = inseriu, 0 = já existia. */
    int insertNew(UUID id, UUID companyId, UUID workflowId, UUID eventId, String eventType,
                  UUID entityId, WorkflowRunStatus status, String conditionsJson, String contextJson);

    void updateStatus(UUID id, UUID companyId, WorkflowRunStatus status, String resultText);

    Optional<WorkflowRun> findById(UUID id, UUID companyId);

    PageResult findByCompanyAndWorkflow(UUID companyId, UUID workflowId, String status,
                                        String eventType, LocalDateTime from, LocalDateTime to,
                                        int page, int pageSize);

    PageResult findByCompany(UUID companyId, String status, String eventType,
                             LocalDateTime from, LocalDateTime to, int page, int pageSize);

    List<RunSummaryRow> summarizeByCompany(UUID companyId);

    record PageResult(List<WorkflowRun> content, long totalElements) {}

    record RunSummaryRow(UUID workflowId, long runCount, String lastStatus,
                         LocalDateTime lastAt, UUID lastEventId) {}
}
