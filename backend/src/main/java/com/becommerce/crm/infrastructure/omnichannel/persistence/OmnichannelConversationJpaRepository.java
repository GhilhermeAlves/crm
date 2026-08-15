package com.becommerce.crm.infrastructure.omnichannel.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OmnichannelConversationJpaRepository extends JpaRepository<OmnichannelConversationJpaEntity, UUID> {

    Optional<OmnichannelConversationJpaEntity> findByCompanyIdAndChannelIdAndExternalPhone(
            UUID companyId, UUID channelId, String externalPhone);

    Page<OmnichannelConversationJpaEntity> findByCompanyIdOrderByLastMessageAtDesc(
            UUID companyId, Pageable pageable);

    long countByCompanyIdAndUnreadCountGreaterThan(UUID companyId, int unreadCount);
}
