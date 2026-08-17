package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.port.output.AiSuggestionProvider;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.MessageType;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSuggestionServiceTest {

    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock OmnichannelMessageRepository messageRepository;
    @Mock AiSuggestionProvider aiSuggestionProvider;

    @InjectMocks AiSuggestionService aiSuggestionService;

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private Conversation conversation() {
        return Conversation.reconstitute(conversationId, companyId, channelId, null, "+5511999999999",
                ConversationStatus.OPEN, null, 0, LocalDateTime.now(), LocalDateTime.now());
    }

    private Message inbound(String body) {
        return Message.createInbound(companyId, conversationId, channelId, "+5511999999999",
                "5511988888888", body, "wamid-" + body.hashCode());
    }

    private Message outbound(String body) {
        return Message.createOutbound(companyId, conversationId, channelId, "5511988888888",
                "+5511999999999", body, UUID.randomUUID());
    }

    @Test
    void shouldSuggestUsingMessageHistory() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversation(conversationId, 0, 40))
                .thenReturn(PageResponse.of(List.of(inbound("Olá, quero saber o preço"), outbound("Tudo bem!")),
                        0, 40, 2));
        when(aiSuggestionProvider.suggest(any())).thenReturn("O preço é R$ 99,90.");
        when(aiSuggestionProvider.providerName()).thenReturn("FAKE");

        var response = aiSuggestionService.suggest(companyId, conversationId);

        assertEquals(conversationId, response.conversationId());
        assertEquals("O preço é R$ 99,90.", response.suggestion());
        assertEquals("FAKE", response.provider());

        ArgumentCaptor<AiSuggestionProvider.SuggestRequest> captor =
                ArgumentCaptor.forClass(AiSuggestionProvider.SuggestRequest.class);
        verify(aiSuggestionProvider).suggest(captor.capture());
        assertEquals(2, captor.getValue().history().size());
        assertEquals("customer", captor.getValue().history().get(0).role());
        assertEquals("assistant", captor.getValue().history().get(1).role());
        assertNull(TenantContext.getCompanyId(), "contexto deve ser limpo");
    }

    @Test
    void shouldRejectConversationFromAnotherCompany() {
        Conversation other = Conversation.reconstitute(conversationId, UUID.randomUUID(), channelId,
                null, "+5511999999999", ConversationStatus.OPEN, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(other));

        assertThrows(OmnichannelNotFoundException.class,
                () -> aiSuggestionService.suggest(companyId, conversationId));
        verify(aiSuggestionProvider, never()).suggest(any());
    }

    @Test
    void shouldRejectMissingConversation() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThrows(OmnichannelNotFoundException.class,
                () -> aiSuggestionService.suggest(companyId, conversationId));
        verify(aiSuggestionProvider, never()).suggest(any());
    }

    @Test
    void shouldSkipBlankMessagesInHistory() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation()));
        Message blank = Message.reconstitute(UUID.randomUUID(), companyId, conversationId, channelId,
                MessageDirection.INBOUND, "+5511999999999", "5511988888888", MessageType.TEXT, "   ",
                MessageStatus.SENT, null, UUID.randomUUID(), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now());
        when(messageRepository.findByConversation(conversationId, 0, 40))
                .thenReturn(PageResponse.of(List.of(blank, inbound("Preciso de ajuda")), 0, 40, 2));
        when(aiSuggestionProvider.suggest(any())).thenReturn("Claro! Como posso ajudar?");
        when(aiSuggestionProvider.providerName()).thenReturn("FAKE");

        var response = aiSuggestionService.suggest(companyId, conversationId);

        ArgumentCaptor<AiSuggestionProvider.SuggestRequest> captor =
                ArgumentCaptor.forClass(AiSuggestionProvider.SuggestRequest.class);
        verify(aiSuggestionProvider).suggest(captor.capture());
        assertEquals(1, captor.getValue().history().size());
        assertNotNull(response.suggestion());
    }
}