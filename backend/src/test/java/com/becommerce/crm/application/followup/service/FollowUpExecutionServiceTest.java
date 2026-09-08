package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.omnichannel.event.WhatsAppSendEvent;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.application.omnichannel.service.OmnichannelMessagePersister;
import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;
import com.becommerce.crm.domain.followup.FollowUpStatus;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationMode;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Executor lógico da fila {@code crm.followup.executor} (Sprint 23): valida
 * conversa/HUMAN/staleness e PREPARA o envio (persist PENDING + publish do
 * {@link WhatsAppSendEvent}) — o envio efetivo/send é do consumer de sender.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FollowUpExecutionServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID followUpId = UUID.randomUUID();

    @Mock FollowUpRepository followUpRepository;
    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock OmnichannelChannelRepository channelRepository;
    @Mock OmnichannelMessageRepository messageRepository;
    @Mock OmnichannelMessagePersister messagePersister;
    @Mock WhatsAppEventPublisher eventPublisher;
    @Mock TenantAuditRecorder auditor;

    private FollowUpExecutionService service;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        service = new FollowUpExecutionService(followUpRepository, conversationRepository,
                channelRepository, messageRepository, messagePersister, eventPublisher, auditor);
        conversation = Conversation.reconstitute(conversationId, companyId, channelId, null,
                "+5511999998888", ConversationStatus.OPEN, ConversationMode.AUTOMATIC, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    }

    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private FollowUp targeted() {
        return FollowUp.reconstitute(followUpId, companyId, conversationId, FollowUpStatus.PROCESSING,
                FollowUpAction.SEND_MESSAGE, "Podemos retomar?", LocalDateTime.now().minusMinutes(5),
                0, null, null, LocalDateTime.now(), null, null, null, null,
                null, LocalDateTime.now(), LocalDateTime.now());
    }

    private Channel channel() {
        return Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-a", null, "secrets-ref",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private FollowUpExecutionEvent event() {
        return FollowUpExecutionEvent.of(companyId, followUpId);
    }

    @Test
    void execute_invalidEvent_shouldRejectSafely() {
        service.execute(null);
        service.execute(FollowUpExecutionEvent.of(null, followUpId));

        verify(followUpRepository, never()).findById(any());
    }

    @Test
    void execute_whenFollowUpMissingOrForeign_shouldDiscard() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.empty());
        service.execute(event());
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void execute_whenNotProcessing_shouldSkipDuplicate() {
        FollowUp done = FollowUp.reconstitute(followUpId, companyId, conversationId,
                FollowUpStatus.SENT, FollowUpAction.SEND_MESSAGE, "x", LocalDateTime.now(),
                0, "wamid-1", null, LocalDateTime.now(), LocalDateTime.now(), null, null, null,
                null, LocalDateTime.now(), LocalDateTime.now());
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(done));

        service.execute(event());

        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void execute_conversationMissing_shouldMarkFailedTerminal() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        service.execute(event());

        verify(followUpRepository).markFailedTerminal(eq(companyId), eq(followUpId),
                eq("Conversa não encontrada"), any());
    }

    @Test
    void execute_humanMode_shouldCancelByRule() {
        conversation.takeover();
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));

        service.execute(event());

        verify(followUpRepository).cancelProcessingByRule(eq(companyId), eq(followUpId),
                eq(FollowUpCancellationReason.HUMAN_MODE), any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void execute_newInboundAfterCreated_shouldCancelAsSuperseded() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(true);

        service.execute(event());

        verify(followUpRepository).cancelProcessingByRule(eq(companyId), eq(followUpId),
                eq(FollowUpCancellationReason.SUPERSEDED_BY_NEW_MESSAGE), any());
    }

    @Test
    void execute_sendMessage_shouldPersistPendingAndPublishSendEventWithFollowUpId() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(messageRepository.findByClientMessageId(followUpId)).thenReturn(Optional.empty());
        Message persisted = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Podemos retomar?", followUpId);
        when(messagePersister.persistPending(any(Message.class))).thenReturn(persisted);

        service.execute(event());

        verify(messagePersister).persistPending(argThat(m -> {
            assertTrue(followUpId.equals(m.getClientMessageId()),
                    "clientMessageId deve ser ancorado no followUpId (idempotência de reexecução)");
            return true;
        }));
        verify(eventPublisher).publishSend(argThat(e ->
                e instanceof WhatsAppSendEvent se && followUpId.equals(se.followUpId())
                        && companyId.equals(se.companyId())
                        && conversationId.equals(se.conversationId())
                        && persisted.getId().equals(se.outboundMessageId())));
    }

    @Test
    void execute_reexecutionWithPendingMessage_shouldRepublishWithoutRePersisting() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        Message existing = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Podemos retomar?", followUpId);
        when(messageRepository.findByClientMessageId(followUpId)).thenReturn(Optional.of(existing));

        service.execute(event());

        verify(messagePersister, never()).persistPending(any());
        verify(eventPublisher).publishSend(argThat(e -> existing.getId().equals(e.outboundMessageId())));
    }

    @Test
    void execute_reexecutionWithHandledMessage_shouldSkip() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        Message done = Message.reconstitute(UUID.randomUUID(), companyId, conversationId, channelId,
                com.becommerce.crm.domain.omnichannel.MessageDirection.OUTBOUND, "espaco-a",
                "+5511999998888", com.becommerce.crm.domain.omnichannel.MessageType.TEXT, "x",
                MessageStatus.SENT, "wamid-1", followUpId, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(messageRepository.findByClientMessageId(followUpId)).thenReturn(Optional.of(done));

        service.execute(event());

        verify(messagePersister, never()).persistPending(any());
        verify(eventPublisher, never()).publishSend(any());
    }

    @Test
    void execute_publishFails_shouldMarkFailedMessageAndScheduleRetry() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(messageRepository.findByClientMessageId(followUpId)).thenReturn(Optional.empty());
        Message persisted = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Podemos retomar?", followUpId);
        when(messagePersister.persistPending(any(Message.class))).thenReturn(persisted);
        doThrow(new IllegalStateException("broker down")).when(eventPublisher).publishSend(any());

        service.execute(event());

        verify(messagePersister).markFailed(eq(persisted.getId()), eq(conversationId), eq("broker down"));
        verify(followUpRepository).scheduleRetry(eq(companyId), eq(followUpId),
                argThat(at -> at.isAfter(LocalDateTime.now())), eq("broker down"), any());
    }

    @Test
    void execute_shouldSetAndClearTenantContext() {
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(targeted()));
        conversation.takeover();

        service.execute(event());

        assertNull(TenantContext.getCompanyId(), "TenantContext deve ser limpo");
    }
}