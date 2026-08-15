package com.becommerce.crm.infrastructure.omnichannel.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OmnichannelChannelJpaRepository extends JpaRepository<OmnichannelChannelJpaEntity, UUID> {

    Optional<OmnichannelChannelJpaEntity> findByCompanyIdAndExternalId(UUID companyId, String externalId);

    List<OmnichannelChannelJpaEntity> findByCompanyIdOrderByNameAsc(UUID companyId);
}
