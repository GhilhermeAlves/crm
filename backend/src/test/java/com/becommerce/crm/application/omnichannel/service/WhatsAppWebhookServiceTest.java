package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelCompanyResolver;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppWebhookParser;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private final String verificationToken = "token-x";
    private final WhatsAppWebhookService service =
            new WhatsAppWebhookService(parser, companyResolver, channelRepository, conversationRepository,
                    messageRepository, contactRepository, eventPublisher, verificationToken);

    private final UUID companyId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();

    private Channel channel() {
        return Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-a", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    private Map<String, Object> inboundPayload() {
        return Map.of("entry", List.of());
    }

    @Test
    void verify_shouldReturnChallengeWhenTokenMatches() {
        when(parser.parseVerification(any()))
                .thenReturn(new WhatsAppWebhookParser.Verification("subscribe", "token-x", "challenge-1"));

        assertEquals("challenge-1", service.verify(Map.of("hub.verify_token", "token-x")));
    }

    @Test
    void verify_shouldReturnNullWhenTokenMismatch() {
        when(parser.parseVerification(any()))
                .thenReturn(new WhatsAppWebhookParser.Verification("subscribe", "other-token", "challenge-1"));
        assertNull(service.verify(Map.of("hub.verify_token", "wrong")));
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

        verify(messageRepository).updateStatusByExternalId("wamid-1", MessageStatus.DELIVERED, null);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void handleEvent_inbound_contactFound_shouldLinkContactId() {
        Contact contact = Contact.reconstitute(UUID.randomUUID(), companyId, "Joao", "Silva", "j@x.com",
                "+5511999998888", null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null);

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