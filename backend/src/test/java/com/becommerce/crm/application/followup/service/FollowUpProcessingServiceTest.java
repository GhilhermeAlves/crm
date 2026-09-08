package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
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
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpProcessingServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID followUpId = UUID.randomUUID();

    @Mock FollowUpRepository followUpRepository;
    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock OmnichannelChannelRepository channelRepository;
    @Mock OmnichannelMessageRepository messageRepository;
    @Mock WhatsAppProvider whatsAppProvider;
    @Mock com.becommerce.crm.application.omnichannel.service.OmnichannelMessagePersister messagePersister;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks FollowUpProcessingService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private FollowUp followUp() {
        return FollowUp.reconstitute(followUpId, companyId, conversationId, FollowUpStatus.PROCESSING,
                FollowUpAction.SEND_MESSAGE, "Podemos retomar?", LocalDateTime.now().minusMinutes(5),
                0, null, null, LocalDateTime.now(), null, null, null, null,
                null, LocalDateTime.now(), LocalDateTime.now());
    }

    private Conversation conversation(ConversationMode mode) {
        return Conversation.reconstitute(conversationId, companyId, channelId, null,
                "+5511999998888", ConversationStatus.OPEN, mode, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private Channel channel() {
        return Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-a", null, "secrets-ref",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void process_whenClaimFails_shouldDoNothing() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(false);

        service.process(companyId, followUpId);

        verify(whatsAppProvider, never()).send(any());
        verify(followUpRepository, never()).markSent(any(), any(), any(), any());
    }

    @Test
    void process_dueFollowUp_shouldExecuteSendMessageAndMarkSent() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(followUp()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.AUTOMATIC)));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(false);
        Message pending = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Podemos retomar?", UUID.randomUUID());
        when(messagePersister.persistPending(any(Message.class))).thenReturn(pending);
        when(whatsAppProvider.send(any(WhatsAppProvider.SendRequest.class)))
                .thenReturn(new WhatsAppProvider.SendResult("wamid-222"));

        service.process(companyId, followUpId);

        verify(whatsAppProvider).send(argThat(r -> r.to().equals("+5511999998888") && r.body().equals("Podemos retomar?")));
        verify(messagePersister).persistPending(any(Message.class));
        verify(messagePersister).markSent(eq(pending.getId()), eq(conversationId), eq("wamid-222"));
        verify(followUpRepository).markSent(eq(companyId), eq(followUpId), eq("wamid-222"), any());
        verify(followUpRepository, never()).cancelProcessingByRule(any(), any(), any(), any());
    }

    @Test
    void process_humanMode_shouldCancelByRuleAndNeverSend() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(followUp()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.HUMAN)));

        service.process(companyId, followUpId);

        verify(followUpRepository).cancelProcessingByRule(eq(companyId), eq(followUpId),
                eq(FollowUpCancellationReason.HUMAN_MODE), any());
        verify(whatsAppProvider, never()).send(any());
        verify(messagePersister, never()).persistPending(any());
    }

    @Test
    void process_newInboundAfterCreated_shouldCancelAsSuperseded() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(followUp()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.AUTOMATIC)));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(true);

        service.process(companyId, followUpId);

        verify(followUpRepository).cancelProcessingByRule(eq(companyId), eq(followUpId),
                eq(FollowUpCancellationReason.SUPERSEDED_BY_NEW_MESSAGE), any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void process_providerFails_shouldRetryWithBackoffWhenAttemptsBelowMax() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(followUp()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.AUTOMATIC)));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(false);
        Message pending = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Podemos retomar?", UUID.randomUUID());
        when(messagePersister.persistPending(any(Message.class))).thenReturn(pending);
        when(whatsAppProvider.send(any(WhatsAppProvider.SendRequest.class)))
                .thenThrow(new OmnichannelProviderException("provider down"));

        service.process(companyId, followUpId);

        verify(messagePersister).markFailed(eq(pending.getId()), eq(conversationId), eq("provider down"));
        verify(followUpRepository).scheduleRetry(eq(companyId), eq(followUpId), argThat(at -> at.isAfter(LocalDateTime.now())),
                eq("provider down"), any());
        verify(followUpRepository, never()).markFailedTerminal(any(), any(), any(), any());
    }

    @Test
    void process_providerFails_shouldMarkFailedTerminalAtMaxAttempts() {
        FollowUp f = FollowUp.reconstitute(followUpId, companyId, conversationId, FollowUpStatus.PROCESSING,
                FollowUpAction.SEND_MESSAGE, "Podemos retomar?", LocalDateTime.now().minusMinutes(5),
                2, null, null, LocalDateTime.now(), null, null, null, null,
                null, LocalDateTime.now(), LocalDateTime.now());
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(f));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation(ConversationMode.AUTOMATIC)));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(messageRepository.existsInboundAfter(eq(conversationId), any())).thenReturn(false);
        Message pending = Message.createOutbound(companyId, conversationId, channelId,
                "espaco-a", "+5511999998888", "Podemos retomar?", UUID.randomUUID());
        when(messagePersister.persistPending(any(Message.class))).thenReturn(pending);
        when(whatsAppProvider.send(any(WhatsAppProvider.SendRequest.class)))
                .thenThrow(new OmnichannelProviderException("provider down"));

        service.process(companyId, followUpId);

        verify(messagePersister).markFailed(eq(pending.getId()), eq(conversationId), eq("provider down"));
        verify(followUpRepository).markFailedTerminal(eq(companyId), eq(followUpId), eq("provider down"), any());
        verify(followUpRepository, never()).scheduleRetry(any(), any(), any(), any(), any());
    }

    @Test
    void process_conversationNotFound_shouldMarkFailedTerminal() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(followUp()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        service.process(companyId, followUpId);

        verify(followUpRepository).markFailedTerminal(eq(companyId), eq(followUpId), eq("Conversa não encontrada"), any());
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void process_followUpFromOtherCompany_shouldNotAct() {
        when(followUpRepository.claim(eq(companyId), eq(followUpId), any(), any())).thenReturn(true);
        FollowUp foreign = FollowUp.reconstitute(followUpId, UUID.randomUUID(), conversationId,
                FollowUpStatus.PROCESSING, FollowUpAction.SEND_MESSAGE, "x", LocalDateTime.now(),
                0, null, null, LocalDateTime.now(), null, null, null, null,
                null, LocalDateTime.now(), LocalDateTime.now());
        when(followUpRepository.findById(followUpId)).thenReturn(Optional.of(foreign));

        service.process(companyId, followUpId);

        verify(whatsAppProvider, never()).send(any());
        verify(followUpRepository, never()).markSent(any(), any(), any(), any());
    }

    @Test
    void backoff_shouldBeProgressive() {
        assertTrue(FollowUpProcessingService.backoff(1).toMinutes() < FollowUpProcessingService.backoff(2).toMinutes());
        assertTrue(FollowUpProcessingService.backoff(2).toMinutes() < FollowUpProcessingService.backoff(3).toMinutes());
        assertEquals(15, FollowUpProcessingService.backoff(1).toMinutes());
    }
}