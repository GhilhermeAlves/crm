package com.becommerce.crm.communication.omnichannel.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OmnichannelIgnoredContactJpaRepository extends JpaRepository<OmnichannelIgnoredContactJpaEntity, UUID> {

    List<OmnichannelIgnoredContactJpaEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    boolean existsByCompanyIdAndPhone(UUID companyId, String phone);

    void deleteByCompanyIdAndId(UUID companyId, UUID id);
}
