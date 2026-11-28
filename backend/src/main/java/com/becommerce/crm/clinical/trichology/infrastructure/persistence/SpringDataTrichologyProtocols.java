package com.becommerce.crm.clinical.trichology.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTrichologyProtocols extends JpaRepository<TrichologyJpaEntities.ProtocolEntity, UUID> {
    List<TrichologyJpaEntities.ProtocolEntity> findByCompanyIdOrderByName(UUID companyId);

    Optional<TrichologyJpaEntities.ProtocolEntity> findByIdAndCompanyId(UUID id, UUID companyId);
}
