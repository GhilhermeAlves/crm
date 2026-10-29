package com.becommerce.crm.clinical.anamnesis.formrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnamnesisOpenLinkJpaRepository extends JpaRepository<AnamnesisOpenLinkJpaEntity, UUID> {

    Optional<AnamnesisOpenLinkJpaEntity> findByCompanyIdAndModelId(UUID companyId, UUID modelId);

    Optional<AnamnesisOpenLinkJpaEntity> findByToken(String token);
}
