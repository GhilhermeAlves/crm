package com.becommerce.crm.application.workflow.port.input;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.workflow.dto.CreateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.DryRunRequest;
import com.becommerce.crm.application.workflow.dto.DryRunResponse;
import com.becommerce.crm.application.workflow.dto.UpdateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowExecutionResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowRunDetailResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowRunResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowRunSummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WorkflowUseCase {

    WorkflowResponse create(UUID companyId, CreateWorkflowRequest request);

    WorkflowResponse getById(UUID companyId, UUID workflowId);

    WorkflowResponse update(UUID companyId, UUID workflowId, UpdateWorkflowRequest request);

    WorkflowResponse activate(UUID companyId, UUID workflowId);

    WorkflowResponse deactivate(UUID companyId, UUID workflowId);

    void delete(UUID companyId, UUID workflowId);

    List<WorkflowResponse> listByCompany(UUID companyId);

    List<WorkflowExecutionResponse> listExecutions(UUID companyId, UUID workflowId);

    List<WorkflowExecutionResponse> listRecentExecutions(UUID companyId);

    PageResponse<WorkflowRunResponse> listRuns(UUID companyId, UUID workflowId, String status,
                                               String eventType, LocalDateTime from, LocalDateTime to,
                                               int page, int pageSize);

    WorkflowRunDetailResponse getRun(UUID companyId, UUID workflowId, UUID runId);

    DryRunResponse dryRun(UUID companyId, UUID workflowId, DryRunRequest request);

    List<WorkflowRunSummary> workflowSummaries(UUID companyId);
}
