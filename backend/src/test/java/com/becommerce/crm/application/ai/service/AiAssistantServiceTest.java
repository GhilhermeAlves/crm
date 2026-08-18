package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.application.ai.port.output.AiChatRepository;
import com.becommerce.crm.application.ai.port.output.AiProvider;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock AiChatRepository chatRepository;
    @Mock AiProvider aiProvider;
    @Mock AiContextResolver contextResolver;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks AiAssistantService aiAssistantService;

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateConversationAndReturnProviderAnswer() {
        when(aiProvider.chat(any())).thenReturn("Resposta do assistente.");
        when(aiProvider.providerName()).thenReturn("FAKE");
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());

        var request = new AiChatRequest("Como está esse cliente?", null,
                new AiContextPayload("customer360", UUID.randomUUID()));

        var response = aiAssistantService.chat(companyId, userId, request);

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
                () -> aiAssistantService.chat(companyId, userId, request));
        verify(aiProvider, never()).chat(any());
    }

    @Test
    void shouldRejectConversationFromAnotherCompany() {
        UUID otherCompany = UUID.randomUUID();
        AiConversation foreign = AiConversation.reconstitute(UUID.randomUUID(), otherCompany, userId,
                "customer360", UUID.randomUUID(), "titulo", LocalDateTime.now(), LocalDateTime.now());
        when(chatRepository.findConversationById(foreign.getId())).thenReturn(Optional.of(foreign));

        var request = new AiChatRequest("oi", foreign.getId(), null);

        assertThrows(AiConversationNotFoundException.class,
                () -> aiAssistantService.chat(companyId, userId, request));
        verify(aiProvider, never()).chat(any());
    }

    @Test
    void shouldRejectMissingConversation() {
        UUID id = UUID.randomUUID();
        when(chatRepository.findConversationById(id)).thenReturn(Optional.empty());

        var request = new AiChatRequest("oi", id, null);

        assertThrows(AiConversationNotFoundException.class,
                () -> aiAssistantService.chat(companyId, userId, request));
        verify(aiProvider, never()).chat(any());
    }

    @Test
    void shouldIncludeResolvedContextInPrompt() {
        String resolvedContext = "Cliente: João\nOportunidades abertas: 2";
        when(contextResolver.resolve(eq(companyId), any())).thenReturn(resolvedContext);
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());
        when(aiProvider.chat(any())).thenReturn("ok");
        when(aiProvider.providerName()).thenReturn("FAKE");

        var request = new AiChatRequest("Como está?", null, new AiContextPayload("customer", UUID.randomUUID()));

        aiAssistantService.chat(companyId, userId, request);

        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider).chat(captor.capture());
        assertTrue(captor.getValue().messages().stream()
                .anyMatch(m -> "system".equals(m.role()) && m.content().contains("João")),
                "contexto resolvido deve entrar no prompt");
        assertEquals(companyId, captor.getValue().companyId());
        assertEquals(userId, captor.getValue().userId());
    }

    @Test
    void shouldNotAddContextWhenResolveReturnsNull() {
        when(contextResolver.resolve(eq(companyId), any())).thenReturn(null);
        when(chatRepository.saveConversation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.findMessagesByConversation(any())).thenReturn(List.of());
        when(aiProvider.chat(any())).thenReturn("ok");
        when(aiProvider.providerName()).thenReturn("FAKE");

        var request = new AiChatRequest("Quais oportunidades estão paradas?", null,
                new AiContextPayload("pipeline", null));

        aiAssistantService.chat(companyId, userId, request);

        ArgumentCaptor<AiProvider.ChatRequest> captor = ArgumentCaptor.forClass(AiProvider.ChatRequest.class);
        verify(aiProvider).chat(captor.capture());
        assertTrue(captor.getValue().messages().stream()
                .noneMatch(m -> "system".equals(m.role()) && !m.content().startsWith("Você é o assistente")),
                "nenhum contexto de registro deve ser injetado quando não há registro em foco");
    }
}