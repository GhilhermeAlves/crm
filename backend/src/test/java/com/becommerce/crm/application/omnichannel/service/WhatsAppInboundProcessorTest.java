package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.omnichannel.event.WhatsAppAutoAiEvent;
import com.becommerce.crm.application.omnichannel.event.WhatsAppInboundEvent;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationMode;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Consumer lógico da fila {@code crm.whatsapp.inbound} (Sprint 23): revalida a
 * conversa e decide se a IA é acionada (preservando Human Takeover), sempre fora
 * do request HTTP do webhook.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WhatsAppInboundProcessorTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock WhatsAppEventPublisher eventPublisher;

    private WhatsAppInboundProcessor processor;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        processor = new WhatsAppInboundProcessor(conversationRepository, eventPublisher);
        conversation = Conversation.reconstitute(conversationId, companyId, UUID.randomUUID(), null,
                "+5511999998888", ConversationStatus.OPEN, ConversationMode.AUTOMATIC, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    }

    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private WhatsAppInboundEvent event() {
        return WhatsAppInboundEvent.of(companyId, conversationId, UUID.randomUUID(),
                UUID.randomUUID(), "wamid-1", "+5511999998888", "Olá");
    }

    @Test
    void execute_invalidEvent_shouldRejectSafely() {
        processor.execute(null);
        processor.execute(new WhatsAppInboundEvent(UUID.randomUUID(), null, conversationId,
                UUID.randomUUID(), UUID.randomUUID(), "wamid-1", "x", "y", LocalDateTime.now()));

        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void execute_conversationMissing_shouldDiscard() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        processor.execute(event());

        verify(eventPublisher, never()).publishAutoAi(any());
    }

    @Test
    void execute_foreignConversation_shouldDiscard() {
        Conversation foreign = Conversation.reconstitute(conversationId, UUID.randomUUID(),
                UUID.randomUUID(), null, "+5511999998888", ConversationStatus.OPEN,
                ConversationMode.AUTOMATIC, null, 0, LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(foreign));

        processor.execute(event());

        verify(eventPublisher, never()).publishAutoAi(any());
    }

    @Test
    void execute_humanMode_shouldNotTriggerAi() {
        conversation.takeover();

        processor.execute(event());

        verify(eventPublisher, never()).publishAutoAi(any());
    }

    @Test
    void execute_automatic_shouldPublishAutoAiEvent() {
        UUID messageId = UUID.randomUUID();
        WhatsAppInboundEvent e = WhatsAppInboundEvent.of(companyId, conversationId, messageId,
                UUID.randomUUID(), "wamid-1", "+5511999998888", "Olá");

        processor.execute(e);

        verify(eventPublisher).publishAutoAi(argThat((WhatsAppAutoAiEvent ai) ->
                ai.companyId().equals(companyId)
                        && ai.conversationId().equals(conversationId)
                        && ai.inboundMessageId().equals(messageId)));
    }

    @Test
    void execute_shouldSetAndClearTenantContext() {
        processor.execute(event());

        assertNull(TenantContext.getCompanyId(), "TenantContext deve ser limpo");
    }
}