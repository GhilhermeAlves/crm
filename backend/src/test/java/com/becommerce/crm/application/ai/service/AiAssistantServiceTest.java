package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.context.AiApplicationContext;
import com.becommerce.crm.application.ai.context.AiCompanyContext;
import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.application.ai.context.AiUserContext;
import com.becommerce.crm.application.ai.context.ResolvedAiContext;
import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.application.ai.port.output.AiChatRepository;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.ai.tool.AiToolRegistry;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.domain.ai.AiConversation;
import com.becommerce.crm.domain.ai.AiConversationNotFoundException;
import com.becommerce.crm.domain.ai.AiMessage;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock AiChatRepository chatRepository;
    @Mock AiProvider aiProvider;
    @Mock AiContextResolver contextResolver;
    @Mock AiToolRegistry toolRegistry;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks AiAssistantService aiAssistantService;

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final List<String> permissions = List.of("contact:read", "opportunity:read");

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private ResolvedAiContext resolved(String crmContext) {
        return new ResolvedAiContext(
                new AiUserContext(userId),
                new AiCompanyContext(companyId),
                new AiPermissionContext(permissions),
                new AiApplicationContext("CUSTOMER", "customer", "/contacts/abc"),
                null,
                crmContext);
    }

    @Test
    void shouldCreateConversationAndReturnProviderAnswer() {
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("Resposta do assistente."));
        when(aiProvider.providerName()).thenReturn("FAKE");
        when(contextResolver.resolve(eq(companyId), eq(userId), eq(permissions), any()))
                .thenReturn(resolved("Cliente: João"));
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());

        var request = new AiChatRequest("Como está esse cliente?", null,
                new AiContextPayload("customer360", UUID.randomUUID()));

        var response = aiAssistantService.chat(companyId, userId, permissions, request);

        assertNotNull(response.conversationId());
        assertEquals("Resposta do assistente.", response.message());
        assertEquals("FAKE", response.provider());

        ArgumentCaptor<AiConversation> convCaptor = ArgumentCaptor.forClass(AiConversation.class);
        verify(chatRepository, atLeastOnce()).saveConversation(convCaptor.capture());
        assertEquals(companyId, convCaptor.getValue().getCompanyId());
        assertEquals(userId, convCaptor.getValue().getUserId());

        ArgumentCaptor<AiMessage> msgCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(chatRepository, times(2)).saveMessage(msgCaptor.capture());
        assertEquals("user", msgCaptor.getAllValues().get(0).getRole());
        assertEquals("assistant", msgCaptor.getAllValues().get(1).getRole());
        assertEquals(companyId, msgCaptor.getAllValues().get(0).getCompanyId());

        verify(auditor).record(eq(companyId), any(), any(), eq("AiConversation"),
                any(), any(), eq(userId), any());
        assertNull(TenantContext.getCompanyId(), "contexto deve ser limpo");
    }

    @Test
    void shouldRejectConversationFromAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        AiConversation foreign = AiConversation.reconstitute(UUID.randomUUID(), companyId, otherUserId,
                "customer360", UUID.randomUUID(), "titulo", LocalDateTime.now(), LocalDateTime.now());
        when(chatRepository.findConversationById(foreign.getId())).thenReturn(Optional.of(foreign));

        var request = new AiChatRequest("oi", foreign.getId(), null);

        assertThrows(AiConversationNotFoundException.class,
                () -> aiAssistantService.chat(companyId, userId, permissions, request));
        verify(aiProvider, never()).chatWithTools(any());
    }

    @Test
    void shouldRejectConversationFromAnotherCompany() {
        UUID otherCompany = UUID.randomUUID();
        AiConversation foreign = AiConversation.reconstitute(UUID.randomUUID(), otherCompany, userId,
                "customer360", UUID.randomUUID(), "titulo", LocalDateTime.now(), LocalDateTime.now());
        when(chatRepository.findConversationById(foreign.getId())).thenReturn(Optional.of(foreign));

        var request = new AiChatRequest("oi", foreign.getId(), null);

        assertThrows(AiConversationNotFoundException.class,
                () -> aiAssistantService.chat(companyId, userId, permissions, request));
        verify(aiProvider, never()).chatWithTools(any());
    }

    @Test
    void shouldRejectMissingConversation() {
        UUID id = UUID.randomUUID();
        when(chatRepository.findConversationById(id)).thenReturn(Optional.empty());

        var request = new AiChatRequest("oi", id, null);

        assertThrows(AiConversationNotFoundException.class,
                () -> aiAssistantService.chat(companyId, userId, permissions, request));
        verify(aiProvider, never()).chatWithTools(any());
    }

    @Test
    void shouldIncludeResolvedContextInPrompt() {
        when(contextResolver.resolve(eq(companyId), eq(userId), eq(permissions), any()))
                .thenReturn(resolved("Cliente: João\nOportunidades abertas: 2"));
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("ok"));
        when(aiProvider.providerName()).thenReturn("FAKE");

        var request = new AiChatRequest("Como está?", null, new AiContextPayload("customer", UUID.randomUUID()));

        aiAssistantService.chat(companyId, userId, permissions, request);

        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider).chatWithTools(captor.capture());
        assertTrue(captor.getValue().messages().stream()
                .anyMatch(m -> "system".equals(m.role()) && m.content().contains("João")),
                "contexto resolvido deve entrar no prompt");
        assertEquals(companyId, captor.getValue().companyId());
        assertEquals(userId, captor.getValue().userId());
    }

    @Test
    void shouldNotAddContextWhenResolveReturnsNull() {
        when(contextResolver.resolve(eq(companyId), eq(userId), eq(permissions), any()))
                .thenReturn(resolved(null));
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());
        when(aiProvider.chatWithTools(any())).thenReturn(AiProvider.ChatResult.content("ok"));
        when(aiProvider.providerName()).thenReturn("FAKE");

        var request = new AiChatRequest("Quais oportunidades estão paradas?", null,
                new AiContextPayload("pipeline", null));

        aiAssistantService.chat(companyId, userId, permissions, request);

        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider).chatWithTools(captor.capture());
        assertTrue(captor.getValue().messages().stream()
                .noneMatch(m -> "system".equals(m.role()) && !m.content().startsWith("Você é o assistente")),
                "nenhum contexto de registro deve ser injetado quando não há registro em foco");
    }

    @Test
    void shouldExecuteToolCallAndReturnFinalAnswer() {
        // 1ª chamada: modelo solicita get_opportunity.
        // 2ª chamada: modelo recebe o resultado e responde o texto final.
        when(aiProvider.chatWithTools(any()))
                .thenReturn(AiProvider.ChatResult.withToolCalls(List.of(
                        new AiProvider.ToolCall("call_1", "get_opportunity", Map.of("opportunityId", "abc")))))
                .thenReturn(AiProvider.ChatResult.content("O cliente desta oportunidade é João Silva."));
        when(aiProvider.providerName()).thenReturn("FAKE");

        AiToolResult toolResult = AiToolResult.ok("get_opportunity",
                Map.of("title", "Proposta A", "contactId", "xyz"));
        when(toolRegistry.execute(eq("get_opportunity"), any(), any())).thenReturn(toolResult);

        when(contextResolver.resolve(eq(companyId), eq(userId), eq(permissions), any()))
                .thenReturn(resolved(null));
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());

        var request = new AiChatRequest("Qual é o cliente desta oportunidade?", null,
                new AiContextPayload("opportunity", "/opportunities/x", "OPPORTUNITY", UUID.randomUUID()));

        var response = aiAssistantService.chat(companyId, userId, permissions, request);

        assertEquals("O cliente desta oportunidade é João Silva.", response.message());

        // A Tool foi executada pelo backend (via registry), não pelo modelo.
        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider, times(2)).chatWithTools(captor.capture());
        verify(toolRegistry).execute(eq("get_opportunity"), any(), any());

        // A auditoria registra a Tool call.
        verify(auditor, atLeastOnce()).record(eq(companyId), any(), any(), eq("AiTool"),
                eq("get_opportunity"), any(), eq(userId), any());
        assertNull(TenantContext.getCompanyId(), "contexto deve ser limpo");
    }

    @Test
    void shouldAuditToolFailureWhenPermissionDenied() {
        when(aiProvider.chatWithTools(any()))
                .thenReturn(AiProvider.ChatResult.withToolCalls(List.of(
                        new AiProvider.ToolCall("call_1", "get_opportunity", Map.of("opportunityId", "abc")))))
                .thenReturn(AiProvider.ChatResult.content("Sem acesso a oportunidades."));
        when(aiProvider.providerName()).thenReturn("FAKE");

        AiToolResult denied = AiToolResult.fail("get_opportunity", "Sem permissão de leitura");
        when(toolRegistry.execute(eq("get_opportunity"), any(), any())).thenReturn(denied);

        when(contextResolver.resolve(eq(companyId), eq(userId), eq(permissions), any()))
                .thenReturn(resolved(null));
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());

        var request = new AiChatRequest("Quais oportunidades estão abertas?", null, null);

        var response = aiAssistantService.chat(companyId, userId, permissions, request);
        assertEquals("Sem acesso a oportunidades.", response.message());
        verify(auditor, atLeastOnce()).record(eq(companyId), any(), any(), eq("AiTool"),
                eq("get_opportunity"), any(), eq(userId), any());
    }
}