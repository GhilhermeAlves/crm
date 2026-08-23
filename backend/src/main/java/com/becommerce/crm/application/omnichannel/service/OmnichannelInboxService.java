package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationDetailResponse;
import com.becommerce.crm.application.omnichannel.dto.ConversationResponse;
import com.becommerce.crm.application.omnichannel.dto.MessageResponse;
import com.becommerce.crm.application.omnichannel.port.input.OmnichannelInboxUseCase;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Inbox omnichannel: listar conversas, detalhar, enviar e marcar como lida (Sprint 16). */
@Service
public class OmnichannelInboxService implements OmnichannelInboxUseCase {

    private static final Logger log = LoggerFactory.getLogger(OmnichannelInboxService.class);

    private final OmnichannelConversationRepository conversationRepository;
    private final OmnichannelMessageRepository messageRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final WhatsAppProvider whatsAppProvider;
    private final OmnichannelMessagePersister messagePersister;

    public OmnichannelInboxService(OmnichannelConversationRepository conversationRepository,
                                   OmnichannelMessageRepository messageRepository,
                                   OmnichannelChannelRepository channelRepository,
                                   WhatsAppProvider whatsAppProvider,
                                   OmnichannelMessagePersister messagePersister) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.whatsAppProvider = whatsAppProvider;
        this.messagePersister = messagePersister;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listConversations(UUID companyId, int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            PageResponse<Conversation> pageResult = conversationRepository.findByCompany(companyId, page, pageSize);
            List<ConversationResponse> content = pageResult.content().stream()
                    .map(c -> new ConversationResponse(c.getId(), c.getChannelId(), c.getContactId(),
                            c.getExternalPhone(), c.getStatus(), c.getLastMessageAt(),
                            messageRepository.findLastBodyByConversation(c.getId()).orElse(null),
                            c.getUnreadCount(), c.getCreatedAt()))
                    .toList();
            return PageResponse.of(content, page, pageSize, pageResult.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UUID companyId, UUID conversationId, int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            Conversation c = requireOwned(companyId, conversationId);
            PageResponse<Message> messages = messageRepository.findByConversation(conversationId, page, pageSize);
            PageResponse<MessageResponse> messageContent = PageResponse.of(
                    messages.content().stream().map(OmnichannelInboxService::toMessageResponse).toList(),
                    page, pageSize, messages.totalElements());
            return new ConversationDetailResponse(c.getId(), c.getChannelId(), c.getContactId(),
                    c.getExternalPhone(), c.getStatus(), c.getLastMessageAt(), c.getUnreadCount(), messageContent);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse send(UUID companyId, UUID conversationId, String body) {
        try {
            TenantContext.setCompanyId(companyId);
            Conversation conversation = requireOwned(companyId, conversationId);
            Channel channel = channelRepository.findById(conversation.getChannelId())
                    .orElseThrow(() -> new OmnichannelNotFoundException(conversation.getChannelId(), "Canal"));

            Message message = Message.createOutbound(companyId, conversationId, channel.getId(),
                    channel.getExternalId(), conversation.getExternalPhone(), body, UUID.randomUUID());
            Message persisted = messagePersister.persistPending(message);

            try {
                WhatsAppProvider.SendResult result = whatsAppProvider.send(
                        new WhatsAppProvider.SendRequest(companyId, channel.getId(),
                                channel.getExternalId(), conversation.getExternalPhone(), body));
                messagePersister.markSent(persisted.getId(), conversationId, result.externalMessageId());
                persisted.markSent(result.externalMessageId());
                return toMessageResponse(persisted);
            } catch (OmnichannelProviderException e) {
                // Persistido em REQUIRES_NEW: sobrevive ao rollback da operação principal.
                messagePersister.markFailed(persisted.getId(), conversationId, e.getMessage());
                log.warn("Falha ao enviar mensagem company={} conversation={}: {}",
                        companyId, conversationId, e.getMessage());
                throw e;
            }
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void markRead(UUID companyId, UUID conversationId) {
        try {
            TenantContext.setCompanyId(companyId);
            Conversation conversation = requireOwned(companyId, conversationId);
            conversation.markRead();
            conversationRepository.save(conversation);
        } finally {
            TenantContext.clear();
        }
    }

    private Conversation requireOwned(UUID companyId, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new OmnichannelNotFoundException(conversationId, "Conversa"));
        if (!conversation.getCompanyId().equals(companyId)) {
            throw new OmnichannelNotFoundException(conversationId, "Conversa");
        }
        return conversation;
    }

    private static MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(m.getId(), m.getConversationId(), m.getDirection(),
                m.getSenderPhone(), m.getRecipientPhone(), m.getType(), m.getBody(),
                m.getStatus(), m.getExternalMessageId(), m.getProviderError(), m.getSentAt(), m.getCreatedAt());
    }
}
