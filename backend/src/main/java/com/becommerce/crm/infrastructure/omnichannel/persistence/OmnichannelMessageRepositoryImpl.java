package com.becommerce.crm.infrastructure.omnichannel.persistence;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelMessageRepository;
import com.becommerce.crm.domain.omnichannel.Message;
import com.becommerce.crm.domain.omnichannel.MessageDirection;
import com.becommerce.crm.domain.omnichannel.MessageStatus;
import com.becommerce.crm.domain.omnichannel.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OmnichannelMessageRepositoryImpl implements OmnichannelMessageRepository {

    private final OmnichannelMessageJpaRepository jpaRepository;

    public OmnichannelMessageRepositoryImpl(OmnichannelMessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Message save(Message message) {
        return toDomain(jpaRepository.save(toEntity(message)));
    }

    @Override
    public Message saveByExternalId(Message message) {
        OmnichannelMessageJpaEntity e = toEntity(message);
        jpaRepository.insertInboundIdempotent(
                e.getId(), e.getCompanyId(), e.getConversationId(), e.getChannelId(), e.getDirection(),
                e.getSenderPhone(), e.getRecipientPhone(), e.getType(), e.getBody(), e.getStatus(),
                e.getExternalMessageId(), e.getClientMessageId(), e.getProviderError(),
                e.getSentAt(), e.getReceivedAt(), e.getCreatedAt(), e.getUpdatedAt());
        // Devolve a linha persistida (a existente se houve conflito) pela chave externa.
        return toDomain(jpaRepository.findByExternalMessageId(message.getExternalMessageId())
                .orElseGet(() -> jpaRepository.save(e)));
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return jpaRepository.findById(id).map(OmnichannelMessageRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Message> findByExternalMessageId(String externalId) {
        return jpaRepository.findByExternalMessageId(externalId).map(OmnichannelMessageRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Message> findByClientMessageId(UUID clientMessageId) {
        return jpaRepository.findByClientMessageId(clientMessageId).map(OmnichannelMessageRepositoryImpl::toDomain);
    }

    @Override
    public PageResponse<Message> findByConversation(UUID conversationId, int page, int pageSize) {
        Page<OmnichannelMessageJpaEntity> result = jpaRepository.findByConversationIdOrderByCreatedAtAsc(
                conversationId, PageRequest.of(page, pageSize));
        return PageResponse.of(result.stream().map(OmnichannelMessageRepositoryImpl::toDomain).toList(),
                page, pageSize, result.getTotalElements());
    }

    @Override
    public Optional<String> findLastBodyByConversation(UUID conversationId) {
        return jpaRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(OmnichannelMessageJpaEntity::getBody);
    }

    @Override
    public void updateStatusByExternalId(String externalId, MessageStatus status, String error) {
        jpaRepository.updateStatusByExternalId(externalId, status.name(), error, LocalDateTime.now());
    }

    private static OmnichannelMessageJpaEntity toEntity(Message m) {
        OmnichannelMessageJpaEntity e = new OmnichannelMessageJpaEntity();
        e.setId(m.getId());
        e.setCompanyId(m.getCompanyId());
        e.setConversationId(m.getConversationId());
        e.setChannelId(m.getChannelId());
        e.setDirection(m.getDirection().name());
        e.setSenderPhone(m.getSenderPhone());
        e.setRecipientPhone(m.getRecipientPhone());
        e.setType(m.getType().name());
        e.setBody(m.getBody());
        e.setStatus(m.getStatus().name());
        e.setExternalMessageId(m.getExternalMessageId());
        e.setClientMessageId(m.getClientMessageId());
        e.setProviderError(m.getProviderError());
        e.setSentAt(m.getSentAt());
        e.setReceivedAt(m.getReceivedAt());
        e.setCreatedAt(m.getCreatedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }

    private static Message toDomain(OmnichannelMessageJpaEntity e) {
        return Message.reconstitute(e.getId(), e.getCompanyId(), e.getConversationId(), e.getChannelId(),
                MessageDirection.valueOf(e.getDirection()), e.getSenderPhone(), e.getRecipientPhone(),
                MessageType.valueOf(e.getType()), e.getBody(), MessageStatus.valueOf(e.getStatus()),
                e.getExternalMessageId(), e.getClientMessageId(), e.getProviderError(),
                e.getSentAt(), e.getReceivedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
