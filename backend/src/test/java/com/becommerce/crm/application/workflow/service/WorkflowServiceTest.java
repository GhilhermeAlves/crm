package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.workflow.dto.CreateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.UpdateWorkflowRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowActionRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowConditionRequest;
import com.becommerce.crm.application.workflow.dto.WorkflowResponse;
import com.becommerce.crm.application.workflow.port.output.WorkflowExecutionRepository;
import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    WorkflowService service;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkflowService(workflowRepository, executionRepository, new ObjectMapper());
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
}