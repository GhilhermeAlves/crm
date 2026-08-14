package com.becommerce.crm.application.workflow.port.input;

import com.becommerce.crm.application.workflow.dto.CreateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.UpdateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowExecutionResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowResponse;

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
}
