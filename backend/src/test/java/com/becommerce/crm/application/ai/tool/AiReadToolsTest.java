package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.application.ai.tool.tools.ActivityTool;
import com.becommerce.crm.application.ai.tool.tools.ContactTool;
import com.becommerce.crm.application.ai.tool.tools.Customer360Tool;
import com.becommerce.crm.application.ai.tool.tools.CustomerTool;
import com.becommerce.crm.application.ai.tool.tools.OpportunityTool;
import com.becommerce.crm.application.ai.tool.tools.PipelineTool;
import com.becommerce.crm.application.ai.tool.tools.SearchActivitiesTool;
import com.becommerce.crm.application.ai.tool.tools.SearchContactsTool;
import com.becommerce.crm.application.ai.tool.tools.SearchOpportunitiesTool;
import com.becommerce.crm.application.ai.tool.tools.SearchTasksTool;
import com.becommerce.crm.application.ai.tool.tools.TaskTool;
import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import com.becommerce.crm.application.customer360.dto.ContactSummaryResponse;
import com.becommerce.crm.application.customer360.dto.Customer360Response;
import com.becommerce.crm.application.customer360.dto.NextActionResponse;
import com.becommerce.crm.application.customer360.service.Customer360Service;
import com.becommerce.crm.application.pipeline.dto.OpportunityResponse;
import com.becommerce.crm.application.pipeline.dto.PipelineResponse;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.application.pipeline.port.input.PipelineUseCase;
import com.becommerce.crm.application.task.dto.TaskResponse;
import com.becommerce.crm.application.task.port.input.TaskUseCase;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.PipelineNotFoundException;
import com.becommerce.crm.domain.task.TaskStatus;
import com.becommerce.crm.domain.task.exception.TaskNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiReadToolsTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("contact:read", "opportunity:read", "activity:read", "task:read", "pipeline:view")));

    private final ContactUseCase contactUseCase = mock(ContactUseCase.class);
    private final OpportunityUseCase opportunityUseCase = mock(OpportunityUseCase.class);
    private final ActivityUseCase activityUseCase = mock(ActivityUseCase.class);
    private final TaskUseCase taskUseCase = mock(TaskUseCase.class);
    private final PipelineUseCase pipelineUseCase = mock(PipelineUseCase.class);
    private final Customer360Service customer360Service = mock(Customer360Service.class);

    @Test
    void shouldGetCustomer() {
        ContactResponse c = new ContactResponse(UUID.randomUUID(), companyId, "João", "Silva",
                "joao@x.com", "+5511", "nota", LocalDateTime.now());
        when(contactUseCase.getById(companyId, c.id())).thenReturn(c);

        var result = new CustomerTool(contactUseCase).execute(ctx, Map.of("customerId", c.id().toString()));

        assertTrue(result.success());
        assertSame(c, result.data());
        assertEquals("get_customer", result.name());
    }

    @Test
    void shouldGetCustomer360() {
        Customer360Response view = new Customer360Response(companyId,
                new ContactSummaryResponse(UUID.randomUUID(), "João", null, null, null, "JS",
                        LocalDateTime.now(), LocalDateTime.now(), false, null),
                0, BigDecimal.ZERO, List.of(), List.of(), List.of(),
                new NextActionResponse("NONE", "Tudo em dia", "ok", 0));
        when(customer360Service.build(companyId, view.contact().id())).thenReturn(view);

        var result = new Customer360Tool(customer360Service)
                .execute(ctx, Map.of("customerId", view.contact().id().toString()));

        assertTrue(result.success());
        assertSame(view, result.data());
    }

    @Test
    void shouldGetContact() {
        ContactResponse c = new ContactResponse(UUID.randomUUID(), companyId, "Ana", "Souza",
                "ana@x.com", null, null, LocalDateTime.now());
        when(contactUseCase.getById(companyId, c.id())).thenReturn(c);

        var result = new ContactTool(contactUseCase).execute(ctx, Map.of("contactId", c.id().toString()));
        assertTrue(result.success());
        assertSame(c, result.data());
    }

    @Test
    void shouldSearchContacts() {
        ContactResponse c = new ContactResponse(UUID.randomUUID(), companyId, "Maria", "Lima",
                "maria@x.com", null, null, LocalDateTime.now());
        when(contactUseCase.search(companyId, "maria", 20)).thenReturn(List.of(c));

        var result = new SearchContactsTool(contactUseCase).execute(ctx, Map.of("query", "maria"));
        assertTrue(result.success());
        assertEquals(List.of(c), result.data());
    }

    @Test
    void shouldGetOpportunity() {
        OpportunityResponse o = new OpportunityResponse(UUID.randomUUID(), companyId, "Proposta A",
                new BigDecimal("10000"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Proposta", 70, null, LocalDateTime.now(), OpportunityStatus.OPEN,
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        when(opportunityUseCase.getById(companyId, o.id())).thenReturn(o);

        var result = new OpportunityTool(opportunityUseCase).execute(ctx, Map.of("opportunityId", o.id().toString()));
        assertTrue(result.success());
        assertSame(o, result.data());
    }

    @Test
    void shouldSearchOpportunitiesOpen() {
        OpportunityResponse o = new OpportunityResponse(UUID.randomUUID(), companyId, "P1",
                new BigDecimal("5000"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Proposta", 50, null, null, OpportunityStatus.OPEN, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(opportunityUseCase.search(companyId, OpportunityStatus.OPEN, null, null, null, null, 20))
                .thenReturn(List.of(o));

        var result = new SearchOpportunitiesTool(opportunityUseCase).execute(ctx, Map.of("status", "OPEN"));
        assertTrue(result.success());
        assertEquals(List.of(o), result.data());
    }

    @Test
    void shouldGetActivity() {
        var a = new com.becommerce.crm.application.activity.dto.ActivityResponse(
                UUID.randomUUID(), companyId, null, null, ActivityType.CALL, "Ligar",
                null, LocalDateTime.now(), userId, LocalDateTime.now(), LocalDateTime.now());
        when(activityUseCase.getById(companyId, a.id())).thenReturn(a);

        var result = new ActivityTool(activityUseCase).execute(ctx, Map.of("activityId", a.id().toString()));
        assertTrue(result.success());
        assertSame(a, result.data());
    }

    @Test
    void shouldSearchActivitiesByContact() {
        var a = new com.becommerce.crm.application.activity.dto.ActivityResponse(
                UUID.randomUUID(), companyId, null, null, ActivityType.NOTE, "Anotação",
                null, LocalDateTime.now(), userId, LocalDateTime.now(), LocalDateTime.now());
        when(activityUseCase.listByContact(companyId, a.id())).thenReturn(List.of(a));

        var result = new SearchActivitiesTool(activityUseCase).execute(ctx, Map.of("contactId", a.id().toString()));
        assertTrue(result.success());
        assertEquals(List.of(a), result.data());
    }

    @Test
    void shouldGetTask() {
        TaskResponse t = new TaskResponse(UUID.randomUUID(), companyId, null, null, "Follow-up",
                null, null, LocalDateTime.now(), null, TaskStatus.PENDING, null, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
        when(taskUseCase.getById(companyId, t.id())).thenReturn(t);

        var result = new TaskTool(taskUseCase).execute(ctx, Map.of("taskId", t.id().toString()));
        assertTrue(result.success());
        assertSame(t, result.data());
    }

    @Test
    void shouldSearchTasksPending() {
        TaskResponse t = new TaskResponse(UUID.randomUUID(), companyId, null, null, "Ligar",
                null, null, null, null, TaskStatus.PENDING, null, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
        when(taskUseCase.listByCompany(companyId, TaskStatus.PENDING)).thenReturn(List.of(t));

        var result = new SearchTasksTool(taskUseCase).execute(ctx, Map.of("status", "PENDING"));
        assertTrue(result.success());
        assertEquals(List.of(t), result.data());
    }

    @Test
    void shouldGetPipeline() {
        PipelineResponse p = new PipelineResponse(UUID.randomUUID(), companyId, "Funil", null, true,
                List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(pipelineUseCase.getById(companyId, p.id())).thenReturn(p);

        var result = new PipelineTool(pipelineUseCase).execute(ctx, Map.of("pipelineId", p.id().toString()));
        assertTrue(result.success());
        assertSame(p, result.data());
    }

    // ------------------------------------------------------------------
    // Segurança: cross-tenant / não encontrado / id inválido / parâmetro ausente
    // ------------------------------------------------------------------

    @Test
    void shouldFailCrossTenantCustomer() {
        UUID foreign = UUID.randomUUID();
        when(contactUseCase.getById(companyId, foreign)).thenThrow(new ContactNotFoundException(foreign));

        var result = new CustomerTool(contactUseCase).execute(ctx, Map.of("customerId", foreign.toString()));
        assertFalse(result.success());
        assertNull(result.data());
        assertTrue(result.error().contains("não encontrado"));
    }

    @Test
    void shouldFailCrossTenantOpportunity() {
        UUID foreign = UUID.randomUUID();
        when(opportunityUseCase.getById(companyId, foreign)).thenThrow(new OpportunityNotFoundException(foreign));

        var result = new OpportunityTool(opportunityUseCase).execute(ctx, Map.of("opportunityId", foreign.toString()));
        assertFalse(result.success());
        assertNull(result.data());
    }

    @Test
    void shouldFailCrossTenantTask() {
        UUID foreign = UUID.randomUUID();
        when(taskUseCase.getById(companyId, foreign)).thenThrow(new TaskNotFoundException(foreign));

        var result = new TaskTool(taskUseCase).execute(ctx, Map.of("taskId", foreign.toString()));
        assertFalse(result.success());
        assertNull(result.data());
    }

    @Test
    void shouldFailCrossTenantPipeline() {
        UUID foreign = UUID.randomUUID();
        when(pipelineUseCase.getById(companyId, foreign)).thenThrow(new PipelineNotFoundException(foreign));

        var result = new PipelineTool(pipelineUseCase).execute(ctx, Map.of("pipelineId", foreign.toString()));
        assertFalse(result.success());
        assertNull(result.data());
    }

    @Test
    void shouldRejectInvalidUuid() {
        var result = new CustomerTool(contactUseCase).execute(ctx, Map.of("customerId", "not-a-uuid"));
        assertFalse(result.success());
    }

    @Test
    void shouldRejectMissingRequiredParam() {
        var result = new CustomerTool(contactUseCase).execute(ctx, Map.of());
        assertFalse(result.success());
        assertTrue(result.error().contains("customerId"));
    }

    @Test
    void shouldRejectInvalidStatus() {
        var result = new SearchOpportunitiesTool(opportunityUseCase).execute(ctx, Map.of("status", "BOGUS"));
        assertFalse(result.success());
    }

    @Test
    void shouldCapExcessiveSearchLimit() {
        ContactResponse c = new ContactResponse(UUID.randomUUID(), companyId, "X", "Y", null, null, null, LocalDateTime.now());
        when(contactUseCase.search(companyId, null, 50)).thenReturn(List.of(c));
        var result = new SearchContactsTool(contactUseCase).execute(ctx, Map.of("limit", 9999));
        assertTrue(result.success());
    }
}