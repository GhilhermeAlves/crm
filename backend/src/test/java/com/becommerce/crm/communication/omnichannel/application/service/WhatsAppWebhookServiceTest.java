package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.identity.application.port.output.EventPublisher;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelCompanyResolver;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelInboxNotifier;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppEventPublisher;
import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageStatus;
import com.becommerce.crm.automation.workflow.domain.event.WorkflowTriggerEvent;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WhatsAppWebhookServiceTest {

    private final WhatsAppWebhookParser parser = mock(WhatsAppWebhookParser.class);
    private final OmnichannelCompanyResolver companyResolver = mock(OmnichannelCompanyResolver.class);
    private final OmnichannelChannelRepository channelRepository = mock(OmnichannelChannelRepository.class);
    private final OmnichannelConversationRepository conversationRepository = mock(OmnichannelConversationRepository.class);
    private final OmnichannelMessageRepository messageRepository = mock(OmnichannelMessageRepository.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final WhatsAppEventPublisher whatsAppEventPublisher = mock(WhatsAppEventPublisher.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final OmnichannelInboxNotifier inboxNotifier = mock(OmnichannelInboxNotifier.class);

    private final WhatsAppWebhookService service =
            new WhatsAppWebhookService(parser, companyResolver, channelRepository, conversationRepository,
                    messageRepository, contactRepository, eventPublisher, whatsAppEventPublisher, jdbcTemplate, inboxNotifier);

    private final UUID companyId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();

    private Channel channel() {
        return Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-a", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    private Map<String, Object> inboundPayload() {
        return Map.of("event", "messages.upsert");
    }

    @Test
    void handleEvent_inbound_shouldPersistMessageAndPublishEvent() {
        when(parser.providerChannelReference(any())).thenReturn("espaco-a");
        when(companyResolver.resolveCompanyByChannelReference("espaco-a")).thenReturn(Optional.of(companyId));
        when(parser.isInboundMessage(any())).thenReturn(true);
        when(parser.parseInboundMessage(any()))
                .thenReturn(Optional.of(new WhatsAppWebhookParser.InboundMessageData(
                        "wamid-1", "+5511999998888", "espaco-a", "Oi")));
        when(messageRepository.findByExternalMessageId("wamid-1")).thenReturn(Optional.empty());
        when(channelRepository.findByCompanyAndExternalId(companyId, "espaco-a"))
                .thenReturn(Optional.of(channel()));
        when(conversationRepository.findByCompanyAndChannelAndPhone(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contactRepository.findByCompanyIdAndPhone(any(), any())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.saveByExternalId(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleEvent(inboundPayload());

        verify(messageRepository).saveByExternalId(any(Message.class));
        verify(conversationRepository, times(2)).save(any(Conversation.class));
        verify(eventPublisher).publish(any(WorkflowTriggerEvent.class));
        verify(whatsAppEventPublisher).publishInbound(any());
        verify(inboxNotifier).notifyNewInbound(eq(companyId), any(), eq("+5511999998888"), eq("Oi"));
    }

    @Test
    void handleEvent_inbound_conversaComNaoLidas_naoNotificaDeNovo() {
        Conversation existing = Conversation.reconstitute(UUID.randomUUID(), companyId, channelId, null,
                "+5511999998888", com.becommerce.crm.communication.omnichannel.domain.ConversationStatus.OPEN,
                java.time.LocalDateTime.now(), 2, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(parser.providerChannelReference(any())).thenReturn("espaco-a");
        when(companyResolver.resolveCompanyByChannelReference("espaco-a")).thenReturn(Optional.of(companyId));
        when(parser.isInboundMessage(any())).thenReturn(true);
        when(parser.parseInboundMessage(any()))
                .thenReturn(Optional.of(new WhatsAppWebhookParser.InboundMessageData(
                        "wamid-3", "+5511999998888", "espaco-a", "mais uma")));
        when(messageRepository.findByExternalMessageId("wamid-3")).thenReturn(Optional.empty());
        when(channelRepository.findByCompanyAndExternalId(companyId, "espaco-a"))
                .thenReturn(Optional.of(channel()));
        when(conversationRepository.findByCompanyAndChannelAndPhone(any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.saveByExternalId(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleEvent(inboundPayload());

        verify(messageRepository).saveByExternalId(any(Message.class));
        verify(inboxNotifier, never()).notifyNewInbound(any(), any(), any(), any());
    }

    @Test
    void handleEvent_inbound_falhaNaNotificacao_naoInterrompeWebhook() {
        when(parser.providerChannelReference(any())).thenReturn("espaco-a");
        when(companyResolver.resolveCompanyByChannelReference("espaco-a")).thenReturn(Optional.of(companyId));
        when(parser.isInboundMessage(any())).thenReturn(true);
        when(parser.parseInboundMessage(any()))
                .thenReturn(Optional.of(new WhatsAppWebhookParser.InboundMessageData(
                        "wamid-4", "+5511999998888", "espaco-a", "Oi")));
        when(messageRepository.findByExternalMessageId("wamid-4")).thenReturn(Optional.empty());
        when(channelRepository.findByCompanyAndExternalId(companyId, "espaco-a"))
                .thenReturn(Optional.of(channel()));
        when(conversationRepository.findByCompanyAndChannelAndPhone(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contactRepository.findByCompanyIdAndPhone(any(), any())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.saveByExternalId(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("db down")).when(inboxNotifier)
                .notifyNewInbound(any(), any(), any(), any());

        service.handleEvent(inboundPayload());

        verify(whatsAppEventPublisher).publishInbound(any());
    }

    @Test
    void handleEvent_inbound_duplicate_shouldSkipPersistenceAndPublish() {
        when(parser.providerChannelReference(any())).thenReturn("espaco-a");
        when(companyResolver.resolveCompanyByChannelReference("espaco-a")).thenReturn(Optional.of(companyId));
        when(parser.isInboundMessage(any())).thenReturn(true);
        when(parser.parseInboundMessage(any()))
                .thenReturn(Optional.of(new WhatsAppWebhookParser.InboundMessageData(
                        "wamid-dup", "+5511999998888", "espaco-a", "Oi")));
        Message existing = Message.createInbound(companyId, UUID.randomUUID(), channelId,
                "+5511999998888", "espaco-a", "Oi", "wamid-dup");
        when(messageRepository.findByExternalMessageId("wamid-dup")).thenReturn(Optional.of(existing));

        service.handleEvent(inboundPayload());

        verify(messageRepository, never()).saveByExternalId(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void handleEvent_status_shouldUpdateStatus() {
        when(parser.providerChannelReference(any())).thenReturn("espaco-a");
        when(companyResolver.resolveCompanyByChannelReference("espaco-a")).thenReturn(Optional.of(companyId));
        when(parser.isInboundMessage(any())).thenReturn(false);
        when(parser.isStatusUpdate(any())).thenReturn(true);
        when(parser.parseStatusUpdate(any()))
                .thenReturn(Optional.of(new WhatsAppWebhookParser.StatusData("wamid-1", MessageStatus.DELIVERED, null)));

        service.handleEvent(inboundPayload());

        verify(messageRepository).updateStatusByExternalId(companyId, "wamid-1", MessageStatus.DELIVERED, null);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void handleEvent_inbound_contactFound_shouldLinkContactId() {
        Contact contact = Contact.reconstitute(UUID.randomUUID(), companyId, "Joao", "Silva", "j@x.com",
                "+5511999998888", null, null,
                null, null, null, null, null, null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null);

        when(parser.providerChannelReference(any())).thenReturn("espaco-a");
        when(companyResolver.resolveCompanyByChannelReference("espaco-a")).thenReturn(Optional.of(companyId));
        when(parser.isInboundMessage(any())).thenReturn(true);
        when(parser.parseInboundMessage(any()))
                .thenReturn(Optional.of(new WhatsAppWebhookParser.InboundMessageData(
                        "wamid-2", "+5511999998888", "espaco-a", "oi")));
        when(messageRepository.findByExternalMessageId("wamid-2")).thenReturn(Optional.empty());
        when(channelRepository.findByCompanyAndExternalId(companyId, "espaco-a"))
                .thenReturn(Optional.of(channel()));
        when(conversationRepository.findByCompanyAndChannelAndPhone(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contactRepository.findByCompanyIdAndPhone(companyId, "+5511999998888"))
                .thenReturn(Optional.of(contact));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.saveByExternalId(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleEvent(inboundPayload());

        verify(eventPublisher).publish(argThat(e ->
                e instanceof WorkflowTriggerEvent wt && contact.getId().equals(wt.contactId())));
    }
}