package com.becommerce.crm.infrastructure.omnichannel.persistence;

import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.domain.omnichannel.Conversation;
import com.becommerce.crm.domain.omnichannel.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OmnichannelConversationRepositoryImpl implements OmnichannelConversationRepository {

    private final OmnichannelConversationJpaRepository jpaRepository;

    public OmnichannelConversationRepositoryImpl(OmnichannelConversationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Conversation save(Conversation conversation) {
        return toDomain(jpaRepository.save(toEntity(conversation)));
    }

    @Override
    public Optional<Conversation> findById(UUID id) {
        return jpaRepository.findById(id).map(OmnichannelConversationRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Conversation> findByCompanyAndChannelAndPhone(UUID companyId, UUID channelId, String externalPhone) {
        return jpaRepository.findByCompanyIdAndChannelIdAndExternalPhone(companyId, channelId, externalPhone)
                .map(OmnichannelConversationRepositoryImpl::toDomain);
    }

    @Override
    public PageResponse<Conversation> findByCompany(UUID companyId, int page, int pageSize) {
        Page<OmnichannelConversationJpaEntity> result = jpaRepository.findByCompanyIdOrderByLastMessageAtDesc(
                companyId, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "lastMessageAt")));
        return PageResponse.of(result.stream().map(OmnichannelConversationRepositoryImpl::toDomain).toList(),
                page, pageSize, result.getTotalElements());
    }

    @Override
    public long countUnreadByCompany(UUID companyId) {
        return jpaRepository.countByCompanyIdAndUnreadCountGreaterThan(companyId, 0);
    }

    private static OmnichannelConversationJpaEntity toEntity(Conversation c) {
        OmnichannelConversationJpaEntity e = new OmnichannelConversationJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setChannelId(c.getChannelId());
        e.setContactId(c.getContactId());
        e.setExternalPhone(c.getExternalPhone());
        e.setStatus(c.getStatus().name());
        e.setLastMessageAt(c.getLastMessageAt());
        e.setUnreadCount(c.getUnreadCount());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    private static Conversation toDomain(OmnichannelConversationJpaEntity e) {
        return Conversation.reconstitute(e.getId(), e.getCompanyId(), e.getChannelId(), e.getContactId(),
                e.getExternalPhone(), ConversationStatus.valueOf(e.getStatus()), e.getLastMessageAt(),
                e.getUnreadCount(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
