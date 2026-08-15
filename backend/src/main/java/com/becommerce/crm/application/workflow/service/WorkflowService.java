package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.workflow.dto.CreateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.ConditionEvaluation;
import com.becommerce.crm.application.workflow.dto.DryRunAction;
import com.becommerce.crm.application.workflow.dto.DryRunRequest;
import com.becommerce.crm.application.workflow.dto.DryRunResponse;
import com.becommerce.crm.application.workflow.dto.DryRunCondition;
import com.becommerce.crm.application.workflow.dto.UpdateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowActionRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowConditionRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowExecutionResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowRunDetailResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowRunResponse;
import com.becommerce.crm.application.workflow.dto.WorkflowRunSummary;
import com.becommerce.crm.application.workflow.port.input.WorkflowUseCase;
import com.becommerce.crm.application.workflow.port.output.WorkflowExecutionRepository;
import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.application.workflow.port.output.WorkflowRunRepository;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.WorkflowCondition;
import com.becommerce.crm.domain.workflow.WorkflowExecution;
import com.becommerce.crm.domain.workflow.WorkflowNotFoundException;
import com.becommerce.crm.domain.workflow.WorkflowRun;
import com.becommerce.crm.domain.workflow.WorkflowRunStatus;
import com.becommerce.crm.domain.workflow.WorkflowValidationException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestão de workflows (Item 8). CRUD scoped à empresa ativa; ativar/desativar
 * via endpoints dedicados. A execução automática fica a cargo do
 * {@link WorkflowExecutor} (sem lógica de execução neste controller).
 *
 * <p>Condições e ações são sempre substituídas por inteiro na edição (valores
 * de baixa cardinalidade, owned pela regra). As ações geram novos ids — o
 * histórico de execução sobrevive, pois workflow_executions não tem FK para
 * workflow_actions (somente a chave de idempotência).
 */
@Service
public class WorkflowService implements WorkflowUseCase {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowRunRepository runRepository;
    private final WorkflowConditionEvaluator conditionEvaluator;
    private final ObjectMapper objectMapper;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowExecutionRepository executionRepository,
                           WorkflowRunRepository runRepository,
                           WorkflowConditionEvaluator conditionEvaluator,
                           ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.runRepository = runRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public WorkflowResponse create(UUID companyId, CreateWorkflowRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            validateAtLeastOneAction(request.actions());
            Workflow workflow = Workflow.create(companyId, request.name(), request.description(), request.trigger());
            buildChildren(workflow, request.conditions(), request.actions());
            workflowRepository.save(workflow);
            return toResponse(workflow);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getById(UUID companyId, UUID workflowId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, workflowId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public WorkflowResponse update(UUID companyId, UUID workflowId, UpdateWorkflowRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            validateAtLeastOneAction(request.actions());
            Workflow workflow = requireOwned(companyId, workflowId);
            workflow.update(request.name(), request.description(), request.trigger());
            buildChildren(workflow, request.conditions(), request.actions());
            workflowRepository.save(workflow);
            return toResponse(workflow);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public WorkflowResponse activate(UUID companyId, UUID workflowId) {
        try {
            TenantContext.setCompanyId(companyId);
            Workflow workflow = requireOwned(companyId, workflowId);
            workflow.activate();
            workflowRepository.save(workflow);
            return toResponse(workflow);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public WorkflowResponse deactivate(UUID companyId, UUID workflowId) {
        try {
            TenantContext.setCompanyId(companyId);
            Workflow workflow = requireOwned(companyId, workflowId);
            workflow.deactivate();
            workflowRepository.save(workflow);
            return toResponse(workflow);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID workflowId) {
        try {
            TenantContext.setCompanyId(companyId);
            Workflow workflow = requireOwned(companyId, workflowId);
            workflowRepository.delete(workflow);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return workflowRepository.findByCompanyId(companyId).stream()
                    .map(WorkflowService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> listExecutions(UUID companyId, UUID workflowId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, workflowId);
            return executionRepository.findByCompanyIdAndWorkflowId(companyId, workflowId).stream()
                    .map(WorkflowService::toExecutionResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowExecutionResponse> listRecentExecutions(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return executionRepository.findByCompanyId(companyId).stream()
                    .map(WorkflowService::toExecutionResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkflowRunResponse> listRuns(UUID companyId, UUID workflowId, String status,
                                                      String eventType, LocalDateTime from, LocalDateTime to,
                                                      int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, workflowId);
            WorkflowRunRepository.PageResult result = runRepository.findByCompanyAndWorkflow(
                    companyId, workflowId, status, eventType, from, to, page, pageSize);
            List<WorkflowRunResponse> content = result.content().stream()
                    .map(WorkflowService::toRunResponse).toList();
            return PageResponse.of(content, page, pageSize, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowRunDetailResponse getRun(UUID companyId, UUID workflowId, UUID runId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, workflowId);
            WorkflowRun run = runRepository.findById(runId, companyId)
                    .filter(r -> r.getWorkflowId().equals(workflowId))
                    .orElseThrow(() -> new WorkflowNotFoundException(runId));
            List<ConditionEvaluation> conditions = readConditions(run.getConditions());
            Map<String, Object> context = readContext(run.getContext());
            List<WorkflowExecutionResponse> actions = executionRepository
                    .findByCompanyIdAndWorkflowIdAndEventId(companyId, workflowId, run.getEventId())
                    .stream().map(WorkflowService::toExecutionResponse).toList();
            return new WorkflowRunDetailResponse(run.getId(), run.getWorkflowId(), run.getEventType(),
                    run.getEntityId(), run.getStatus(), run.getResultText(),
                    run.getCreatedAt(), run.getUpdatedAt(), conditions, context, actions);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DryRunResponse dryRun(UUID companyId, UUID workflowId, DryRunRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Workflow workflow = requireOwned(companyId, workflowId);
            String eventType = request.eventType() != null ? request.eventType().trim() : null;
            Map<String, Object> context = request.context() != null ? request.context() : Collections.emptyMap();

            List<DryRunCondition> conditions = workflow.getConditions().stream()
                    .map(c -> {
                        Object actual = context.get(c.getField());
                        boolean matched = conditionEvaluator.matches(c.getField(), c.getOperator(), c.getValue(), actual);
                        return new DryRunCondition(c.getField(), c.getOperator(), c.getValue(), actual, matched);
                    })
                    .toList();
            boolean matched = conditions.stream().allMatch(DryRunCondition::matched);

            List<DryRunAction> actions = matched
                    ? workflow.getActions().stream()
                            .sorted(java.util.Comparator.comparingInt(WorkflowAction::getSortOrder))
                            .map(a -> new DryRunAction(a.getActionType(), a.getSortOrder(), parseConfig(a.getConfig())))
                            .toList()
                    : List.of();

            String message = matched
                    ? "Workflow elegível. " + actions.size() + " ação(ões) seriam executadas."
                    : "Workflow não elegível — alguma condição não foi atendida.";
            return new DryRunResponse(matched, eventType, conditions, actions, message);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowRunSummary> workflowSummaries(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return runRepository.summarizeByCompany(companyId).stream()
                    .map(row -> {
                        String lastError = null;
                        WorkflowRunStatus lastStatus = row.lastStatus() != null
                                ? WorkflowRunStatus.valueOf(row.lastStatus()) : null;
                        if ((lastStatus == WorkflowRunStatus.FAILED || lastStatus == WorkflowRunStatus.PARTIAL)
                                && row.lastEventId() != null) {
                            lastError = executionRepository
                                    .findByCompanyIdAndWorkflowIdAndEventId(companyId, row.workflowId(), row.lastEventId())
                                    .stream().map(WorkflowExecution::getErrorMessage)
                                    .filter(m -> m != null && !m.isBlank())
                                    .findFirst().orElse(null);
                        }
                        return new WorkflowRunSummary(row.workflowId(), row.runCount(), lastStatus,
                                row.lastAt(), lastError);
                    })
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    private Workflow requireOwned(UUID companyId, UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        if (!workflow.getCompanyId().equals(companyId)) {
            throw new WorkflowNotFoundException(workflowId);
        }
        return workflow;
    }

    private void validateAtLeastOneAction(List<WorkflowActionRequest> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new WorkflowValidationException("Pelo menos uma ação é obrigatória.");
        }
    }

    private void buildChildren(Workflow workflow, List<WorkflowConditionRequest> conditions,
                               List<WorkflowActionRequest> actions) {
        List<WorkflowCondition> newConditions = new ArrayList<>();
        if (conditions != null) {
            int i = 0;
            for (WorkflowConditionRequest c : conditions) {
                newConditions.add(WorkflowCondition.create(workflow.getCompanyId(), workflow.getId(),
                        c.field(), c.operator(), c.value(), c.sortOrder() != 0 ? c.sortOrder() : i));
                i++;
            }
        }
        workflow.replaceConditions(newConditions);

        List<WorkflowAction> newActions = new ArrayList<>();
        int i = 0;
        for (WorkflowActionRequest a : actions) {
            newActions.add(WorkflowAction.create(workflow.getCompanyId(), workflow.getId(),
                    a.actionType(), a.sortOrder() != 0 ? a.sortOrder() : i, serializeConfig(a.config())));
            i++;
        }
        workflow.replaceActions(newActions);
    }

    private String serializeConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new WorkflowValidationException("Configuração de ação inválida.");
        }
    }

    private static WorkflowResponse toResponse(Workflow w) {
        List<WorkflowResponse.ConditionResponse> conditions = w.getConditions().stream()
                .map(c -> new WorkflowResponse.ConditionResponse(c.getId(), c.getField(),
                        c.getOperator(), c.getValue(), c.getSortOrder()))
                .toList();
        List<WorkflowResponse.ActionResponse> actions = w.getActions().stream()
                .map(a -> new WorkflowResponse.ActionResponse(a.getId(), a.getActionType(),
                        a.getSortOrder(), a.getConfig()))
                .toList();
        return new WorkflowResponse(w.getId(), w.getCompanyId(), w.getName(), w.getDescription(),
                w.getTrigger().name(), w.isActive(), conditions, actions,
                w.getCreatedAt(), w.getUpdatedAt());
    }

    private static WorkflowExecutionResponse toExecutionResponse(WorkflowExecution e) {
        return new WorkflowExecutionResponse(e.getId(), e.getWorkflowId(), e.getActionType(),
                e.getEventType(), e.getEntityId(), e.getStatus(), e.getResultText(),
                e.getErrorMessage(), e.getCreatedAt());
    }

    private static WorkflowRunResponse toRunResponse(WorkflowRun r) {
        return new WorkflowRunResponse(r.getId(), r.getWorkflowId(), r.getEventType(), r.getEntityId(),
                r.getStatus(), r.getResultText(), r.getCreatedAt(), r.getUpdatedAt());
    }

    @SuppressWarnings("unchecked")
    private List<ConditionEvaluation> readConditions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ConditionEvaluation.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readContext(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}