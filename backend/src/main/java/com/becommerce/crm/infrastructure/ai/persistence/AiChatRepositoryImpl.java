package com.becommerce.crm.infrastructure.ai.persistence;

import com.becommerce.crm.application.ai.port.output.AiChatRepository;
import com.becommerce.crm.domain.ai.AiConversation;
import com.becommerce.crm.domain.ai.AiMessage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AiChatRepositoryImpl implements AiChatRepository {

    private final AiConversationJpaRepository conversationJpaRepository;
    private final AiMessageJpaRepository messageJpaRepository;

    public AiChatRepositoryImpl(AiConversationJpaRepository conversationJpaRepository,
                                AiMessageJpaRepository messageJpaRepository) {
        this.conversationJpaRepository = conversationJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
    }

    @Override
    public AiConversation saveConversation(AiConversation conversation) {
        return toDomain(conversationJpaRepository.save(toEntity(conversation)));
    }

    @Override
    public Optional<AiConversation> findConversationById(UUID id) {
        return conversationJpaRepository.findById(id).map(AiChatRepositoryImpl::toDomain);
    }

    @Override
    public List<AiConversation> findConversationsByUser(UUID companyId, UUID userId) {
        return conversationJpaRepository.findByCompanyIdAndUserIdOrderByUpdatedAtDesc(companyId, userId).stream()
                .map(AiChatRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public AiMessage saveMessage(AiMessage message) {
        return toDomain(messageJpaRepository.save(toEntity(message)));
    }

    @Override
    public List<AiMessage> findMessagesByConversation(UUID conversationId) {
        return messageJpaRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(AiChatRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public void deleteConversation(UUID id) {
        conversationJpaRepository.deleteById(id);
    }

    private static AiConversationJpaEntity toEntity(AiConversation c) {
        AiConversationJpaEntity e = new AiConversationJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setUserId(c.getUserId());
        e.setScreen(c.getScreen());
        e.setRecordId(c.getRecordId());
        e.setTitle(c.getTitle());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    private static AiConversation toDomain(AiConversationJpaEntity e) {
        return AiConversation.reconstitute(
                e.getId(), e.getCompanyId(), e.getUserId(), e.getScreen(), e.getRecordId(),
                e.getTitle(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static AiMessageJpaEntity toEntity(AiMessage m) {
        AiMessageJpaEntity e = new AiMessageJpaEntity();
        e.setId(m.getId());
        e.setCompanyId(m.getCompanyId());
        e.setConversationId(m.getConversationId());
        e.setRole(m.getRole());
        e.setContent(m.getContent());
        e.setCreatedAt(m.getCreatedAt());
        return e;
    }

    private static AiMessage toDomain(AiMessageJpaEntity e) {
        return AiMessage.reconstitute(
                e.getId(), e.getCompanyId(), e.getConversationId(), e.getRole(),
                e.getContent(), e.getCreatedAt());
    }
}
