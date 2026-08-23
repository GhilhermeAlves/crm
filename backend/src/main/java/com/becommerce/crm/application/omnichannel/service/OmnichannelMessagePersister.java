package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistência do ciclo de vida da mensagem enviada (Sprint 16).
 *
 * <p>Cada transição roda em transação própria ({@code REQUIRES_NEW}), seguindo o
 * padrão de {@code WorkflowActionRunner}: quando o provider falha, a exceção
 * interrompe a operação principal, mas o registro {@code FAILED} já foi commitado
 * em transação separada e não é perdido por rollback.
 */
@Component
public class OmnichannelMessagePersister {

    private final OmnichannelMessageRepository messageRepository;
    private final OmnichannelConversationRepository conversationRepository;

    public OmnichannelMessagePersister(OmnichannelMessageRepository messageRepository,
                                       OmnichannelConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Message persistPending(Message message) {
        return messageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID messageId, UUID conversationId, String externalMessageId) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.markSent(externalMessageId);
            messageRepository.save(m);
        });
        touchConversation(conversationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID messageId, UUID conversationId, String error) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.markStatus(MessageStatus.FAILED, error);
            messageRepository.save(m);
        });
        touchConversation(conversationId);
    }

    private void touchConversation(UUID conversationId) {
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.touch(LocalDateTime.now(), false);
            conversationRepository.save(c);
        });
    }
}
