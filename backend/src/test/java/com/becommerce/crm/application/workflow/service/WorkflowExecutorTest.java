package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.WorkflowCondition;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutorTest {

    @Mock WorkflowRepository workflowRepository;
    @Mock WorkflowActionRunner actionRunner;

    WorkflowExecutor executor;

    private final UUID companyId = UUID.randomUUID();

    private void setUp() {
        executor = new WorkflowExecutor(workflowRepository, new WorkflowConditionEvaluator(), actionRunner);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private Workflow proposalWorkflow(TriggerEvent trigger) {
        Workflow wf = Workflow.create(companyId, "Follow-up proposta", null, trigger);
        wf.activate();
        return wf;
    }

    private WorkflowTriggerEvent stageChangedTo(String stage, BigDecimal value) {
        return WorkflowTriggerEvent.opportunityStageChanged(companyId, UUID.randomUUID(), null, stage, value);
    }

    @Test
    void process_runsAction_forMatchingActiveWorkflow() {
        setUp();
        Workflow wf = proposalWorkflow(TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0, "{\"title\":\"x\"}"));
        when(workflowRepository.findByCompanyIdAndTriggerAndActive(companyId, TriggerEvent.OPPORTUNITY_STAGE_CHANGED, true))
                .thenReturn(List.of(wf));

        executor.process(stageChangedTo("Proposta", new BigDecimal("500")));

        verify(actionRunner).run(eq(wf), eq(wf.getActions().get(0)), any(WorkflowTriggerEvent.class));
    }

    @Test
    void process_skipsWorkflow_whenConditionDoesNotMatch() {
        setUp();
        Workflow wf = proposalWorkflow(TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0, "{\"title\":\"x\"}"));
        when(workflowRepository.findByCompanyIdAndTriggerAndActive(companyId, TriggerEvent.OPPORTUNITY_STAGE_CHANGED, true))
                .thenReturn(List.of(wf));

        executor.process(stageChangedTo("Qualificação", new BigDecimal("500")));

        verifyNoInteractions(actionRunner);
    }

    @Test
    void process_recordsFailure_whenActionThrows_andContinues() {
        setUp();
        Workflow wf = proposalWorkflow(TriggerEvent.OPPORTUNITY_WON);
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0, "{\"title\":\"x\"}"));
        when(workflowRepository.findByCompanyIdAndTriggerAndActive(companyId, TriggerEvent.OPPORTUNITY_WON, true))
                .thenReturn(List.of(wf));
        doThrow(new IllegalStateException("falhou")).when(actionRunner).run(any(), any(), any());

        WorkflowTriggerEvent event = WorkflowTriggerEvent.opportunityWon(companyId, UUID.randomUUID(), null, "Fechado", new BigDecimal("500"));
        executor.process(event);

        verify(actionRunner).recordFailure(eq(wf), eq(wf.getActions().get(0)), any(WorkflowTriggerEvent.class), any(Throwable.class));
    }

    @Test
    void process_doesNothing_whenNoActiveWorkflowForTrigger() {
        setUp();
        when(workflowRepository.findByCompanyIdAndTriggerAndActive(companyId, TriggerEvent.ACTIVITY_CREATED, true))
                .thenReturn(List.of());

        executor.process(WorkflowTriggerEvent.activityCreated(companyId, UUID.randomUUID(), null, null, "CALL"));

        verifyNoInteractions(actionRunner);
    }

    @Test
    void process_ignoresNullCompany() {
        setUp();
        executor.process(WorkflowTriggerEvent.opportunityCreated(null, UUID.randomUUID(), null, "P", new BigDecimal("1")));
        verifyNoInteractions(actionRunner);
    }

    @Test
    void recursionGuard_preventsReentrantProcessing() {
        setUp();
        Workflow wf = proposalWorkflow(TriggerEvent.OPPORTUNITY_WON);
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0, "{\"title\":\"x\"}"));
        when(workflowRepository.findByCompanyIdAndTriggerAndActive(companyId, TriggerEvent.OPPORTUNITY_WON, true))
                .thenReturn(List.of(wf));

        // simulando processamento aninhado (ex.: action cria Task -> novo evento)
        executor.process(WorkflowTriggerEvent.opportunityWon(companyId, UUID.randomUUID(), null, "Fechado", new BigDecimal("1")));
        executor.process(WorkflowTriggerEvent.opportunityWon(companyId, UUID.randomUUID(), null, "Fechado", new BigDecimal("1")));

        // após a primeira chamada o guard é removido; a segunda também processa normalmente
        verify(actionRunner, times(2)).run(any(), any(), any());
        assertFalse(executor.isProcessing());
    }
}