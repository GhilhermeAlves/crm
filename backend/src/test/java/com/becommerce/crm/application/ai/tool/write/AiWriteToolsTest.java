package com.becommerce.crm.application.ai.tool.write;

import com.becommerce.crm.application.ai.action.AiActionService;
import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Write tools (AI-05): validam argumentos/vinculos e criam PROPOSTAS - nunca
 * executam a escrita. Verifica a chamada a {@link AiActionService#propose} com
 * os parametros tipados e a rejeicao de vinculos cross-tenant/nao encontrados.
 */
class AiWriteToolsTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("task:create", "activity:create", "opportunity:update")),
            conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);

    private final CreateTaskTool taskTool = new CreateTaskTool(actionService, contactRepository, opportunityRepository);
    private final CreateActivityTool activityTool = new CreateActivityTool(actionService, contactRepository, opportunityRepository);
    private final UpdateOpportunityTool opportunityTool = new UpdateOpportunityTool(actionService, opportunityRepository);

    @BeforeEach
    void setUp() {
        AiActionResponse proposal = new AiActionResponse(UUID.randomUUID(), conversationId, "create_task",
                "TASK", null, "Criar tarefa: X", "PROPOSED", Map.of(), null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(actionService.propose(eq(companyId), eq(userId), eq(conversationId), anyString(), anyString(),
                any(), any(), anyString())).thenReturn(proposal);
    }

    @Test
    void shouldProposeCreateTask() {
        var result = taskTool.execute(ctx, Map.of("title", "Ligar para Joao", "priority", "HIGH"));

        assertTrue(result.success());
        assertInstanceOf(AiActionResponse.class, result.data());
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId),
                eq("create_task"), eq("TASK"), isNull(), any(), anyString());
    }

    @Test
    void shouldRejectTaskWithoutTitle() {
        assertThrows(IllegalArgumentException.class, () -> taskTool.execute(ctx, Map.of()));
    }

    @Test
    void shouldRejectTaskTitleTooLong() {
        assertThrows(IllegalArgumentException.class, () ->
                taskTool.execute(ctx, Map.of("title", "x".repeat(201))));
    }

    @Test
    void shouldRejectTaskWithForeignContact() {
        UUID foreign = UUID.randomUUID();
        when(contactRepository.findById(foreign)).thenThrow(new ContactNotFoundException(foreign));
        assertThrows(ContactNotFoundException.class, () ->
                taskTool.execute(ctx, Map.of("title", "T", "contactId", foreign.toString())));
    }

    @Test
    void shouldRejectTaskWithInactiveContact() {
        UUID inactive = UUID.randomUUID();
        Contact contact = Contact.reconstitute(inactive, companyId, "A", "B", "a@x.com", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(contactRepository.findById(inactive)).thenReturn(java.util.Optional.of(contact));
        assertThrows(ContactNotFoundException.class, () ->
                taskTool.execute(ctx, Map.of("title", "T", "contactId", inactive.toString())));
    }

    @Test
    void shouldAcceptTaskWithOwnedActiveContact() {
        UUID owned = UUID.randomUUID();
        Contact contact = Contact.reconstitute(owned, companyId, "A", "B", "a@x.com", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null);
        when(contactRepository.findById(owned)).thenReturn(java.util.Optional.of(contact));
        var result = taskTool.execute(ctx, Map.of("title", "T", "contactId", owned.toString()));
        assertTrue(result.success());
    }

    @Test
    void shouldRejectTaskWithForeignOpportunity() {
        UUID foreign = UUID.randomUUID();
        when(opportunityRepository.findById(foreign)).thenThrow(new OpportunityNotFoundException(foreign));
        assertThrows(OpportunityNotFoundException.class, () ->
                taskTool.execute(ctx, Map.of("title", "T", "opportunityId", foreign.toString())));
    }

    @Test
    void shouldProposeCreateActivity() {
        var result = activityTool.execute(ctx, Map.of("type", "CALL", "subject", "Ligar"));

        assertTrue(result.success());
        assertInstanceOf(AiActionResponse.class, result.data());
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId),
                eq("create_activity"), eq("ACTIVITY"), isNull(), any(), anyString());
    }

    @Test
    void shouldRejectActivityWithoutType() {
        assertThrows(IllegalArgumentException.class, () ->
                activityTool.execute(ctx, Map.of("subject", "Ligar")));
    }

    @Test
    void shouldRejectActivityWithoutSubject() {
        assertThrows(IllegalArgumentException.class, () ->
                activityTool.execute(ctx, Map.of("type", "CALL")));
    }

    @Test
    void shouldRejectActivityInvalidType() {
        assertThrows(IllegalArgumentException.class, () ->
                activityTool.execute(ctx, Map.of("type", "BOGUS", "subject", "Ligar")));
    }

    @Test
    void shouldProposeUpdateOpportunity() {
        UUID oppId = UUID.randomUUID();
        Opportunity opportunity = Opportunity.create(companyId, "Proposta A", new BigDecimal("1000"),
                null, UUID.randomUUID(), UUID.randomUUID(), null, null, null);
        when(opportunityRepository.findById(oppId)).thenReturn(java.util.Optional.of(opportunity));

        var result = opportunityTool.execute(ctx, Map.of(
                "opportunityId", oppId.toString(),
                "title", "Proposta A atualizada",
                "value", "2000"));

        assertTrue(result.success());
        assertInstanceOf(AiActionResponse.class, result.data());
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId),
                eq("update_opportunity"), eq("OPPORTUNITY"), eq(oppId), any(), anyString());
    }

    @Test
    void shouldRejectUpdateWithoutOpportunityId() {
        assertThrows(IllegalArgumentException.class, () ->
                opportunityTool.execute(ctx, Map.of("title", "X")));
    }

    @Test
    void shouldRejectUpdateForeignOpportunity() {
        UUID foreign = UUID.randomUUID();
        when(opportunityRepository.findById(foreign)).thenThrow(new OpportunityNotFoundException(foreign));
        assertThrows(OpportunityNotFoundException.class, () ->
                opportunityTool.execute(ctx, Map.of("opportunityId", foreign.toString())));
    }
}