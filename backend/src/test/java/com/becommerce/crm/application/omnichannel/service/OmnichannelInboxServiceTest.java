package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OmnichannelInboxServiceTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    @Mock OmnichannelConversationRepository conversationRepository;
    @Mock OmnichannelMessageRepository messageRepository;
    @Mock OmnichannelChannelRepository channelRepository;
    @Mock WhatsAppProvider whatsAppProvider;
    @Mock OmnichannelMessagePersister messagePersister;

    @InjectMocks OmnichannelInboxService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private Conversation conversation() {
        return Conversation.reconstitute(UUID.randomUUID(), companyId, channelId, contactId,
                "+5511999998888", com.becommerce.crm.domain.omnichannel.ConversationStatus.OPEN,
                null, 0, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    private Channel channel() {
        return Channel.reconstitute(channelId, companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-a", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    private Message outbound() {
        return Message.createOutbound(companyId, UUID.randomUUID(), channelId,
                "espaco-a", "+5511999998888", "Bom dia!", UUID.randomUUID());
    }

    @Test
    void send_shouldPersistPendingThenMarkSent() {
        Conversation c = conversation();
        when(conversationRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(whatsAppProvider.send(any(WhatsAppProvider.SendRequest.class)))
                .thenReturn(new WhatsAppProvider.SendResult("wamid-1"));
        Message pending = outbound();
        when(messagePersister.persistPending(any(Message.class))).thenReturn(pending);

        service.send(companyId, c.getId(), "Bom dia!");

        verify(whatsAppProvider).send(any(WhatsAppProvider.SendRequest.class));
        verify(messagePersister).persistPending(any(Message.class));
        verify(messagePersister).markSent(eq(pending.getId()), eq(c.getId()), eq("wamid-1"));
        verify(messagePersister, never()).markFailed(any(), any(), any());
    }

    @Test
    void send_whenProviderFails_shouldPersistFailedInNewTransactionAndThrow() {
        Conversation c = conversation();
        when(conversationRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel()));
        when(whatsAppProvider.send(any(WhatsAppProvider.SendRequest.class)))
                .thenThrow(new OmnichannelProviderException("131026: n invalid"));
        Message pending = outbound();
        when(messagePersister.persistPending(any(Message.class))).thenReturn(pending);

        assertThrows(OmnichannelProviderException.class, () -> service.send(companyId, c.getId(), "oi"));
        verify(messagePersister).persistPending(any(Message.class));
        verify(messagePersister).markFailed(eq(pending.getId()), eq(c.getId()), eq("131026: n invalid"));
        verify(messagePersister, never()).markSent(any(), any(), any());
    }

    @Test
    void send_whenConversationFromOtherCompany_shouldThrowNotFound() {
        Conversation other = Conversation.reconstitute(UUID.randomUUID(), UUID.randomUUID(), channelId, null,
                "+5511999998888", com.becommerce.crm.domain.omnichannel.ConversationStatus.OPEN,
                null, 0, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(conversationRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThrows(OmnichannelNotFoundException.class,
                () -> service.send(companyId, other.getId(), "oi"));
        verify(whatsAppProvider, never()).send(any());
    }

    @Test
    void listConversations_shouldReturnPagedWithLastBodyAndUnread() {
        Conversation c = conversation();
        PageResponse<Conversation> page = PageResponse.of(List.of(c), 0, 20, 1);
        when(conversationRepository.findByCompany(companyId, 0, 20)).thenReturn(page);
        when(messageRepository.findLastBodyByConversation(c.getId())).thenReturn(Optional.of("Bom dia!"));

        PageResponse<ConversationResponse> result = service.listConversations(companyId, 0, 20);

        assertEquals(1, result.totalElements());
        assertEquals("Bom dia!", result.content().get(0).lastMessage());
        assertEquals(c.getExternalPhone(), result.content().get(0).externalPhone());
    }

    @Test
    void markRead_shouldClearUnread() {
        Conversation c = conversation();
        when(conversationRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markRead(companyId, c.getId());
        assertEquals(0, c.getUnreadCount());
    }
}