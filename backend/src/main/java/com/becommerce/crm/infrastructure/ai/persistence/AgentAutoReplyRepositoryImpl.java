package com.becommerce.crm.infrastructure.ai.persistence;

import com.becommerce.crm.application.ai.port.output.AgentAutoReplyRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentAutoReplyRepositoryImpl implements AgentAutoReplyRepository {

    private final AgentAutoReplyJpaRepository jpaRepository;

    public AgentAutoReplyRepositoryImpl(AgentAutoReplyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean reserve(UUID companyId, UUID conversationId, UUID inboundMessageId) {
        LocalDateTime now = LocalDateTime.now();
        int inserted = jpaRepository.reserve(UUID.randomUUID(), companyId, conversationId,
                inboundMessageId, now);
        return inserted > 0;
    }

    @Override
    public Optional<LocalDateTime> lastAutoReplyAt(UUID companyId, UUID conversationId) {
        return jpaRepository
                .findFirstByCompanyIdAndConversationIdOrderByRepliedAtDesc(companyId, conversationId)
                .map(AgentAutoReplyJpaEntity::getRepliedAt);
    }
}