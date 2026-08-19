package com.becommerce.crm.application.ai.action;

import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.output.AiActionRepository;
import com.becommerce.crm.application.ai.port.output.AiChatRepository;
import com.becommerce.crm.application.ai.tool.AiTool;
import com.becommerce.crm.application.ai.tool.AiToolRegistry;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.domain.ai.AiAction;
import com.becommerce.crm.domain.ai.AiActionInvalidStateException;
import com.becommerce.crm.domain.ai.AiActionNotFoundException;
import com.becommerce.crm.domain.ai.AiActionStatus;
import com.becommerce.crm.domain.ai.AiConversation;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida de acoes de escrita (AI-05): proposta, confirmacao (com
 * idempotencia, permissoes, posse e concorrencia), cancelamento e listagem.
 */
class AiActionServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUser = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    private final AiActionRepository actionRepository = mock(AiActionRepository.class);
    private final AiChatRepository chatRepository = mock(AiChatRepository.class);
    private final AiToolRegistry toolRegistry = mock(AiToolRegistry.class);
    private final AiActionExecutor executor = mock(AiActionExecutor.class);
    private final TenantAuditRecorder auditor = mock(TenantAuditRecorder.class);

    private AiActionService service;

    @BeforeEach
    void setUp() {
        service = new AiActionService(actionRepository, chatRepository, toolRegistry, executor, auditor);
        AiConversation conversation = AiConversation.reconstitute(conversationId, companyId, userId,
                "screen", null, "titulo", java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(chatRepository.findConversationById(conversationId))
                .thenReturn(Optional.of(conversation));
        AiTool tool = mock(AiTool.class);
        when(tool.requiredPermission()).thenReturn("task:create");
        when(toolRegistry.find("create_task")).thenReturn(Optional.of(tool));
    }

    private AiAction proposedAction() {
        return AiAction.propose(companyId, userId, conversationId, "create_task", "TASK", null,
                Map.of("title", "Ligar"), "Criar tarefa: Ligar");
    }

    // ---------------------------------------------------------------- propose

    @Test
    void shouldProposeAndPersistProposal() {
        when(actionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiActionResponse response = service.propose(companyId, userId, conversationId, "create_task",
                "TASK", null, Map.of("title", "Ligar"), "Criar tarefa: Ligar");

        assertEquals("PROPOSED", response.status());
        verify(actionRepository).save(any());
        verify(auditor).record(eq(companyId), any(), any(), eq("AiAction"), any(), any(), eq(userId), any());
    }

    @Test
    void shouldRejectProposeForForeignConversation() {
        UUID foreign = UUID.randomUUID();
        AiConversation conv = AiConversation.reconstitute(foreign, companyId, otherUser, null, null,
                "t", java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(chatRepository.findConversationById(foreign)).thenReturn(Optional.of(conv));

        assertThrows(AiActionNotFoundException.class, () -> service.propose(
                companyId, userId, foreign, "create_task", "TASK", null, Map.of(), "d"));
    }

    // ---------------------------------------------------------------- confirm

    @Test
    void shouldConfirmAndExecuteProposal() {
        AiAction action = proposedAction();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));
        when(actionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(executor.execute(any())).thenReturn(Map.of("id", UUID.randomUUID().toString(), "title", "Ligar"));

        AiActionResponse response = service.confirm(companyId, userId,
                List.of("ai:chat", "task:create"), action.getId());

        assertEquals("EXECUTED", response.status());
        verify(executor).execute(action);
    }

    @Test
    void shouldBeIdempotentWhenAlreadyExecuted() {
        AiAction action = proposedAction();
        action.markExecuting();
        action.markExecuted(Map.of("title", "Ligar"));
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        AiActionResponse response = service.confirm(companyId, userId,
                List.of("ai:chat", "task:create"), action.getId());

        assertEquals("EXECUTED", response.status());
        verify(executor, never()).execute(any());
    }

    @Test
    void shouldBeIdempotentWhenFailed() {
        AiAction action = proposedAction();
        action.markFailed("erro");
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        AiActionResponse response = service.confirm(companyId, userId,
                List.of("ai:chat", "task:create"), action.getId());

        assertEquals("FAILED", response.status());
        verify(executor, never()).execute(any());
    }

    @Test
    void shouldDenyConfirmWithoutBusinessPermission() {
        AiAction action = proposedAction();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        assertThrows(CrmAccessDeniedException.class, () ->
                service.confirm(companyId, userId, List.of("ai:chat"), action.getId()));
    }

    @Test
    void shouldDenyConfirmForDifferentUser() {
        AiAction action = proposedAction();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        assertThrows(AiActionNotFoundException.class, () ->
                service.confirm(companyId, otherUser, List.of("ai:chat", "task:create"), action.getId()));
    }

    @Test
    void shouldDenyConfirmForForeignCompany() {
        AiAction action = AiAction.propose(UUID.randomUUID(), userId, conversationId, "create_task",
                "TASK", null, Map.of(), "d");
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        assertThrows(AiActionNotFoundException.class, () ->
                service.confirm(companyId, userId, List.of("ai:chat", "task:create"), action.getId()));
    }

    @Test
    void shouldRejectConfirmWhenCancelled() {
        AiAction action = proposedAction();
        action.cancel();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        assertThrows(AiActionInvalidStateException.class, () ->
                service.confirm(companyId, userId, List.of("ai:chat", "task:create"), action.getId()));
    }

    @Test
    void shouldMarkFailedOnExecutionError() {
        AiAction action = proposedAction();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));
        when(actionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(executor.execute(any())).thenThrow(new IllegalArgumentException("Valor invalido"));

        AiActionResponse response = service.confirm(companyId, userId,
                List.of("ai:chat", "task:create"), action.getId());

        assertEquals("FAILED", response.status());
        assertNull(response.result());
    }

    @Test
    void shouldReturnNotFoundForUnknownAction() {
        UUID missing = UUID.randomUUID();
        when(actionRepository.findByIdForUpdate(missing)).thenReturn(Optional.empty());

        assertThrows(AiActionNotFoundException.class, () ->
                service.confirm(companyId, userId, List.of("ai:chat"), missing));
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void shouldCancelProposedAction() {
        AiAction action = proposedAction();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));
        when(actionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiActionResponse response = service.cancel(companyId, userId, action.getId());

        assertEquals("CANCELLED", response.status());
        assertEquals(AiActionStatus.CANCELLED, action.getStatus());
    }

    @Test
    void shouldRejectCancelWhenNotProposed() {
        AiAction action = proposedAction();
        action.markExecuting();
        action.markExecuted(Map.of("x", "y"));
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        assertThrows(AiActionInvalidStateException.class, () ->
                service.cancel(companyId, userId, action.getId()));
    }

    @Test
    void shouldDenyCancelForDifferentUser() {
        AiAction action = proposedAction();
        when(actionRepository.findByIdForUpdate(action.getId())).thenReturn(Optional.of(action));

        assertThrows(AiActionNotFoundException.class, () ->
                service.cancel(companyId, otherUser, action.getId()));
    }

    // ---------------------------------------------------------------- list

    @Test
    void shouldListActionsOfOwnedConversation() {
        AiAction action = proposedAction();
        when(actionRepository.findByConversationId(conversationId)).thenReturn(List.of(action));

        List<AiActionResponse> result = service.listByConversation(companyId, userId, conversationId);

        assertEquals(1, result.size());
        assertEquals(action.getId(), result.get(0).id());
    }

    @Test
    void shouldRejectListForForeignConversation() {
        UUID foreign = UUID.randomUUID();
        AiConversation conv = AiConversation.reconstitute(foreign, companyId, otherUser, null, null,
                "t", java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(chatRepository.findConversationById(foreign)).thenReturn(Optional.of(conv));

        assertThrows(AiActionNotFoundException.class, () ->
                service.listByConversation(companyId, userId, foreign));
    }
}