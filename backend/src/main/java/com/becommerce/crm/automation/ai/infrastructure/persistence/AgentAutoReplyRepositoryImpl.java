package com.becommerce.crm.automation.ai.infrastructure.persistence;

import com.becommerce.crm.automation.ai.application.port.output.AgentAutoReplyRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentAutoReplyRepositoryImpl implements AgentAutoReplyRepository {

    private final AgentAutoReplyJpaRepository jpaRepository;

    public AgentAutoReplyRepositoryImpl(AgentAutoReplyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** INSERT nativo (@Modifying) exige transação; o consumer Rabbit não abre uma. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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