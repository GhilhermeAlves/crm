package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnamnesisModelJpaRepository extends JpaRepository<AnamnesisModelJpaEntity, UUID> {

    List<AnamnesisModelJpaEntity> findByCompanyIdOrderByNameAsc(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
