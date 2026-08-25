package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.activity.dto.ActivityResponse;
import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
import com.becommerce.crm.application.task.dto.TaskResponse;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.application.workflow.port.output.WorkflowExecutionRepository;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.task.TaskPriority;
import com.becommerce.crm.domain.task.TaskStatus;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ExecutionStatus;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowActionRunnerTest {

    @Mock WorkflowExecutionRepository executionRepository;
    @Mock TaskUseCase taskUseCase;
    @Mock ActivityUseCase activityUseCase;
    @Mock com.becommerce.crm.application.notification.port.input.NotificationUseCase notificationUseCase;
    @Mock com.becommerce.crm.application.campaign.port.input.CampaignUseCase campaignUseCase;

    WorkflowActionRunner runner;

    private final UUID companyId = UUID.randomUUID();
    private final UUID opportunityId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        runner = new WorkflowActionRunner(executionRepository, taskUseCase, activityUseCase,
                notificationUseCase, campaignUseCase, new ObjectMapper());
    }

    private Workflow workflow() {
        return Workflow.create(companyId, "Follow-up após proposta", null, TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
    }

    private WorkflowTriggerEvent stageChangedEvent() {
        return WorkflowTriggerEvent.opportunityStageChanged(companyId, opportunityId, contactId, "Proposta",
                new BigDecimal("5000"));
    }

    private TaskResponse taskResponse(UUID id) {
        return new TaskResponse(id, companyId, contactId, opportunityId, "Follow-up", null, null,
                LocalDateTime.now(), TaskPriority.HIGH, TaskStatus.PENDING, null, WorkflowActionRunner.SYSTEM_ACTOR,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private ActivityResponse activityResponse(UUID id) {
        return new ActivityResponse(id, companyId, contactId, opportunityId, ActivityType.CALL, "Ligar", null,
                LocalDateTime.now(), WorkflowActionRunner.SYSTEM_ACTOR, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void run_createTask_SuccessRecordsResult() {
        WorkflowAction action = WorkflowAction.create(companyId, workflow().getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up\",\"dueInDays\":2,\"priority\":\"HIGH\"}");
        UUID createdTaskId = UUID.randomUUID();
        when(executionRepository.insertNew(any(), eq(companyId), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(taskUseCase.create(any(), any(), eq(WorkflowActionRunner.SYSTEM_ACTOR)))
                .thenReturn(taskResponse(createdTaskId));

        runner.run(workflow(), action, stageChangedEvent());

        ArgumentCaptor<UUID> execId = ArgumentCaptor.forClass(UUID.class);
        verify(executionRepository).updateResult(execId.capture(), eq(companyId), eq(ExecutionStatus.SUCCESS),
                contains("Task criada"), isNull());
        assertNotNull(execId.getValue());
    }

    @Test
    void run_includesWorkflowAttributionInDescription() {
        WorkflowAction action = WorkflowAction.create(companyId, workflow().getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up\"}");
        when(executionRepository.insertNew(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(taskUseCase.create(any(), any(), any())).thenReturn(taskResponse(UUID.randomUUID()));

        runner.run(workflow(), action, stageChangedEvent());

        ArgumentCaptor<com.becommerce.crm.application.task.dto.CreateTaskRequest> req =
                ArgumentCaptor.forClass(com.becommerce.crm.application.task.dto.CreateTaskRequest.class);
        verify(taskUseCase).create(any(), req.capture(), any());
        assertNotNull(req.getValue().description());
        assertTrue(req.getValue().description().contains("Criada automaticamente pelo workflow"),
                "Transparência: a tarefa deve registrar a origem automática (Item 11)");
        assertEquals(opportunityId, req.getValue().opportunityId());
    }

    @Test
    void run_createActivity_Success() {
        WorkflowAction action = WorkflowAction.create(companyId, workflow().getId(), ActionType.CREATE_ACTIVITY, 0,
                "{\"subject\":\"Ligar para cliente\",\"type\":\"CALL\"}");
        when(executionRepository.insertNew(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(activityUseCase.create(any(), any(), eq(WorkflowActionRunner.SYSTEM_ACTOR)))
                .thenReturn(activityResponse(UUID.randomUUID()));

        runner.run(workflow(), action, stageChangedEvent());

        verify(executionRepository).updateResult(any(), eq(companyId), eq(ExecutionStatus.SUCCESS),
                contains("Activity criada"), isNull());
    }

    @Test
    void run_alreadyProcessed_skipsWithoutExecuting() {
        WorkflowAction action = WorkflowAction.create(companyId, workflow().getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up\"}");
        when(executionRepository.insertNew(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        runner.run(workflow(), action, stageChangedEvent());

        verifyNoInteractions(taskUseCase);
        verify(executionRepository, never()).updateResult(any(), any(), any(), any(), any());
    }

    @Test
    void run_actionFailure_propagatesAndNoResultUpdate() {
        WorkflowAction action = WorkflowAction.create(companyId, workflow().getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up\"}");
        when(executionRepository.insertNew(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(taskUseCase.create(any(), any(), any())).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> runner.run(workflow(), action, stageChangedEvent()));
        verify(executionRepository, never()).updateResult(any(), any(), eq(ExecutionStatus.SUCCESS), any(), any());
    }

    @Test
    void recordFailure_recordsFailedWithErrorMessage() {
        WorkflowAction action = WorkflowAction.create(companyId, workflow().getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up\"}");
        when(executionRepository.insertNew(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        runner.recordFailure(workflow(), action, stageChangedEvent(), new IllegalStateException("Contato inválido"));

        verify(executionRepository).updateResult(any(), eq(companyId), eq(ExecutionStatus.FAILED),
                isNull(), contains("Contato inválido"));
    }
}