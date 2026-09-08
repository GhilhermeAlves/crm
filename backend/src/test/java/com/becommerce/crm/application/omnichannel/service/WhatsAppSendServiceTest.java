package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.followup.service.FollowUpSendOutcomeHandler;
import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationMode;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.MessageType;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sender lógico da fila {@code crm.whatsapp.sender} (Sprint 23): único ponto
 * de ENVIO efetivo via {@link WhatsAppProvider} (UAZAPI) fora do request HTTP.
 * Cobre idempotência por estado (apenas PENDING), desfecho de mensagem e de
 * follow-up, e o retry/DLQ (regra de falha: provider → FAILED; runtime → propaga).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WhatsAppSendServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();
    private final UUID followUpId = UUID.randomUUID();

    @Mock OmnichannelMessageRepository messageRepository;
    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock OmnichannelChannelRepository channelRepository;
    @Mock WhatsAppProvider whatsAppProvider;
    @Mock OmnichannelMessagePersister messagePersister;
    @Mock FollowUpSendOutcomeHandler followUpOutcome;

    private WhatsAppSendService service;

    private Conversation conversation;
    private Channel channel;

    @BeforeEach
    void setUp() {
        service = new WhatsAppSendService(messageRepository, conversationRepository,
                channelRepository, whatsAppProvider, messagePersister, followUpOutcome);
        conversation = Conversation.reconstitute(conversationId, companyId, channelId, null,
                "+5511999998888", ConversationStatus.OPEN, ConversationMode.AUTOMATIC, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        channel = Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-a", null, "secrets-ref",
                LocalDateTime.now(), LocalDateTime.now());
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(whatsAppProvider.send(any())).thenReturn(new WhatsAppProvider.SendResult("wamid-1"));
        when(whatsAppProvider.providerName()).thenReturn("FAKE");
    }

    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private Message pendingMessage() {
        return Message.reconstitute(messageId, companyId, conversationId, channelId,
                MessageDirection.OUTBOUND, "espaco-a", "+5511999998888", MessageType.TEXT,
                "Olá!", MessageStatus.PENDING, null, UUID.randomUUID(), null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Message sentMessage() {
        return Message.reconstitute(messageId, companyId, conversationId, channelId,
                MessageDirection.OUTBOUND, "espaco-a", "+5511999998888", MessageType.TEXT,
                "Olá!", MessageStatus.SENT, "wamid-1", UUID.randomUUID(), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private WhatsAppSendEvent event(UUID followUp) {
        return WhatsAppSendEvent.ofFollowUp(companyId, conversationId, messageId, channelId,
                "+5511999998888", "Olá!", followUp);
    }

    @Test
    void send_invalidEvent_shouldRejectSafely() {
        service.send(null);
        service.send(WhatsAppSendEvent.of(null, conversationId, messageId, channelId, "x", "y"));

        verify(messageRepository, never()).findById(any());
    }

    @Test
    void send_messageMissingOrForeign_shouldDiscard() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        service.send(event(null));

        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void send_notPending_shouldSkipDuplicate() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(sentMessage()));

        service.send(event(null));

        verify(whatsAppProvider, never()).send(any());
        verify(messagePersister, never()).markSent(any(), any(), any());
    }

    @Test
    void send_conversationMissing_shouldFailPermanentlyWithoutRetry() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(pendingMessage()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        service.send(event(followUpId));

        verify(messagePersister).markFailed(eq(messageId), eq(conversationId), any());
        verify(followUpOutcome).onSendFailed(eq(companyId), eq(followUpId), any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void send_channelMissing_shouldFailPermanentlyWithoutRetry() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(pendingMessage()));
        when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

        service.send(event(null));

        verify(messagePersister).markFailed(eq(messageId), eq(conversationId), any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void send_success_shouldProviderSendMarkSentAndFollowUpOutcome() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(pendingMessage()));

        service.send(event(followUpId));

        verify(whatsAppProvider).send(argThat(r ->
                r.companyId().equals(companyId)
                        && r.channelId().equals(channelId)
                        && "espaco-a".equals(r.phoneNumberId())
                        && "+5511999998888".equals(r.to())
                        && "Olá!".equals(r.body())
                        && "secrets-ref".equals(r.secretsRef())));
        verify(messagePersister).markSent(eq(messageId), eq(conversationId), eq("wamid-1"));
        verify(followUpOutcome).markSent(eq(companyId), eq(followUpId), eq("wamid-1"));
    }

    @Test
    void send_success_withoutFollowUp_shouldNotTouchFollowUp() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(pendingMessage()));

        service.send(event(null));

        verify(messagePersister).markSent(eq(messageId), eq(conversationId), eq("wamid-1"));
        verify(followUpOutcome, never()).markSent(any(), any(), any());
    }

    @Test
    void send_providerFails_shouldMarkFailedAndRouteFollowUpBackoff() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(pendingMessage()));
        doThrow(new OmnichannelProviderException("provider down")).when(whatsAppProvider).send(any());

        service.send(event(followUpId));

        verify(messagePersister).markFailed(eq(messageId), eq(conversationId), eq("provider down"));
        verify(followUpOutcome).onSendFailed(eq(companyId), eq(followUpId), eq("provider down"));
        verify(messagePersister, never()).markSent(any(), any(), any());
    }

    @Test
    void send_runtimeFailure_shouldPropagateForRabbitRetry() {
        when(messageRepository.findById(messageId))
                .thenThrow(new IllegalStateException("db down"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.send(event(null)));
    }

    @Test
    void send_shouldSetAndClearTenantContext() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(pendingMessage()));

        service.send(event(null));

        assertNull(TenantContext.getCompanyId(), "TenantContext deve ser limpo");
    }
}