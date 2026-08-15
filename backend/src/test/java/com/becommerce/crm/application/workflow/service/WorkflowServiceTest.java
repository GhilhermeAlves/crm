package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.workflow.dto.CreateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.DryRunRequest;
import com.becommerce.crm.application.workflow.dto.UpdateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowActionRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowConditionRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowResponse;
import com.becommerce.crm.application.workflow.port.output.WorkflowExecutionRepository;
import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.application.workflow.port.output.WorkflowRunRepository;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.ExecutionStatus;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowExecution;
import com.becommerce.crm.domain.workflow.WorkflowNotFoundException;
import com.becommerce.crm.domain.workflow.WorkflowRun;
import com.becommerce.crm.domain.workflow.WorkflowRunStatus;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowExecutionRepository executionRepository;
    @Mock WorkflowRunRepository runRepository;

    WorkflowService service;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkflowService(workflowRepository, executionRepository, runRepository,
                new WorkflowConditionEvaluator(), new ObjectMapper());
        lenient().when(workflowRepository.save(any(Workflow.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private CreateWorkflowRequest request(String name, TriggerEvent trigger) {
        return new CreateWorkflowRequest(name, "desc", trigger,
                List.of(new WorkflowConditionRequest(null, "opportunity.stage",
                        ConditionOperator.EQUALS, "Proposta", 0)),
                List.of(new WorkflowActionRequest(null, ActionType.CREATE_TASK, 0,
                        Map.of("title", "Follow-up", "dueInDays", 2))));
    }

    @Test
    void create_buildsWorkflowAndPersists() {
        CreateWorkflowRequest request = request("Follow-up proposta", TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        WorkflowResponse response = service.create(companyId, request);

        assertNotNull(response.id());
        assertEquals(companyId, response.companyId());
        assertEquals("Follow-up proposta", response.name());
        assertFalse(response.active(), "Novo workflow deve nascer inativo");
        assertEquals(1, response.conditions().size());
        assertEquals(1, response.actions().size());
        assertEquals(ActionType.CREATE_TASK, response.actions().get(0).actionType());

        ArgumentCaptor<Workflow> captor = ArgumentCaptor.forClass(Workflow.class);
        verify(workflowRepository).save(captor.capture());
        assertEquals(TriggerEvent.OPPORTUNITY_STAGE_CHANGED, captor.getValue().getTrigger());
        assertEquals(1, captor.getValue().getConditions().size());
        assertEquals(1, captor.getValue().getActions().size());
    }

    @Test
    void create_requiresAtLeastOneAction() {
        CreateWorkflowRequest noActions = new CreateWorkflowRequest("x", null, TriggerEvent.OPPORTUNITY_WON,
                List.of(), List.of());
        assertThrows(RuntimeException.class, () -> service.create(companyId, noActions));
    }

    @Test
    void activate_flipsFlag() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        WorkflowResponse response = service.activate(companyId, existing.getId());
        assertTrue(response.active());
        verify(workflowRepository).save(existing);
    }

    @Test
    void deactivate_flipsFlagOff() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        existing.activate();
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        WorkflowResponse response = service.deactivate(companyId, existing.getId());
        assertFalse(response.active());
    }

    @Test
    void getFromAnotherCompany_throwsNotFound() {
        Workflow existing = Workflow.create(UUID.randomUUID(), "x", null, TriggerEvent.OPPORTUNITY_WON);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThrows(WorkflowNotFoundException.class, () -> service.getById(companyId, existing.getId()));
    }

    @Test
    void delete_removesWorkflow() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.delete(companyId, existing.getId());
        verify(workflowRepository).delete(existing);
    }

    @Test
    void update_replacesChildren() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        UpdateWorkflowRequest update = new UpdateWorkflowRequest("y", null, TriggerEvent.OPPORTUNITY_WON,
                List.of(),
                List.of(new WorkflowActionRequest(null, ActionType.CREATE_TASK, 0, Map.of("title", "Novo"))));

        service.update(companyId, existing.getId(), update);
        verify(workflowRepository).save(existing);
        assertEquals("y", existing.getName());
        assertEquals(0, existing.getConditions().size());
        assertEquals(1, existing.getActions().size());
    }

    @Test
    void dryRun_reportsConditionsAndWouldRunActions_whenMatched() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        existing.replaceConditions(List.of(
                com.becommerce.crm.domain.workflow.WorkflowCondition.create(companyId, existing.getId(),
                        "opportunity.stage", ConditionOperator.EQUALS, "Proposta", 0)));
        existing.replaceActions(List.of(
                com.becommerce.crm.domain.workflow.WorkflowAction.create(companyId, existing.getId(),
                        ActionType.CREATE_TASK, 0, "{\"title\":\"x\"}")));

        var response = service.dryRun(companyId, existing.getId(),
                new DryRunRequest("OPPORTUNITY_STAGE_CHANGED", Map.of("opportunity.stage", "Proposta")));

        assertTrue(response.matched());
        assertEquals(1, response.conditions().size());
        assertTrue(response.conditions().get(0).matched());
        assertEquals(1, response.actions().size());
        assertEquals(ActionType.CREATE_TASK, response.actions().get(0).actionType());
    }

    @Test
    void dryRun_doesNotReportActions_whenConditionFails() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        existing.replaceConditions(List.of(
                com.becommerce.crm.domain.workflow.WorkflowCondition.create(companyId, existing.getId(),
                        "opportunity.stage", ConditionOperator.EQUALS, "Proposta", 0)));
        existing.replaceActions(List.of(
                com.becommerce.crm.domain.workflow.WorkflowAction.create(companyId, existing.getId(),
                        ActionType.CREATE_TASK, 0, "{\"title\":\"x\"}")));

        var response = service.dryRun(companyId, existing.getId(),
                new DryRunRequest("OPPORTUNITY_STAGE_CHANGED", Map.of("opportunity.stage", "Qualificação")));

        assertFalse(response.matched());
        assertEquals(0, response.actions().size());
    }

    @Test
    void getRun_returnsDetailWithConditionsContextAndActions() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        UUID runId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        WorkflowRun run = WorkflowRun.reconstitute(runId, companyId, existing.getId(), eventId, "OPPORTUNITY_WON",
                entityId, WorkflowRunStatus.SUCCESS, "[]", "{\"opportunity.stage\":\"Proposta\"}",
                "ok", LocalDateTime.now(), LocalDateTime.now());
        when(runRepository.findById(runId, companyId)).thenReturn(Optional.of(run));
        WorkflowExecution exec = WorkflowExecution.reconstitute(UUID.randomUUID(), companyId, existing.getId(),
                UUID.randomUUID(), eventId, "OPPORTUNITY_WON", entityId, ActionType.CREATE_TASK,
                ExecutionStatus.SUCCESS, "ok", null, LocalDateTime.now(), LocalDateTime.now());
        when(executionRepository.findByCompanyIdAndWorkflowIdAndEventId(companyId, existing.getId(), eventId))
                .thenReturn(List.of(exec));

        var detail = service.getRun(companyId, existing.getId(), runId);

        assertEquals(runId, detail.id());
        assertEquals(WorkflowRunStatus.SUCCESS, detail.status());
        assertEquals(1, detail.actions().size());
        assertEquals("Proposta", detail.context().get("opportunity.stage"));
    }

    @Test
    void listRuns_paginatesContent() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        when(workflowRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        UUID runId = UUID.randomUUID();
        WorkflowRun run = WorkflowRun.reconstitute(runId, companyId, existing.getId(), UUID.randomUUID(),
                "OPPORTUNITY_WON", UUID.randomUUID(), WorkflowRunStatus.SUCCESS, "[]", "{}",
                "ok", LocalDateTime.now(), LocalDateTime.now());
        when(runRepository.findByCompanyAndWorkflow(companyId, existing.getId(), null, null,
                null, null, 0, 20))
                .thenReturn(new WorkflowRunRepository.PageResult(List.of(run), 1L));

        PageResponse<com.becommerce.crm.application.workflow.dto.WorkflowRunResponse> page =
                service.listRuns(companyId, existing.getId(), null, null, null, null, 0, 20);

        assertEquals(1, page.content().size());
        assertEquals(runId, page.content().get(0).id());
        assertEquals(1, page.totalElements());
    }

    @Test
    void workflowSummaries_resolvesLastErrorForFailedRun() {
        Workflow existing = Workflow.create(companyId, "x", null, TriggerEvent.OPPORTUNITY_WON);
        UUID eventId = UUID.randomUUID();
        when(runRepository.summarizeByCompany(companyId)).thenReturn(List.of(
                new WorkflowRunRepository.RunSummaryRow(existing.getId(), 3, "FAILED",
                        LocalDateTime.now(), eventId)));
        WorkflowExecution failedExec = WorkflowExecution.reconstitute(UUID.randomUUID(), companyId, existing.getId(),
                UUID.randomUUID(), eventId, "OPPORTUNITY_WON", UUID.randomUUID(), ActionType.CREATE_TASK,
                ExecutionStatus.FAILED, null, "contato inválido", LocalDateTime.now(), LocalDateTime.now());
        when(executionRepository.findByCompanyIdAndWorkflowIdAndEventId(companyId, existing.getId(), eventId))
                .thenReturn(List.of(failedExec));

        var summaries = service.workflowSummaries(companyId);

        assertEquals(1, summaries.size());
        assertEquals(existing.getId(), summaries.get(0).workflowId());
        assertEquals(3, summaries.get(0).runCount());
        assertEquals("contato inválido", summaries.get(0).lastError());
    }
}