package com.becommerce.crm.sales.chair.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChairJpaRepository extends JpaRepository<ChairJpaEntity, UUID> {

    List<ChairJpaEntity> findByCompanyIdOrderBySortOrderAscNameAsc(UUID companyId);

    Optional<ChairJpaEntity> findByCompanyIdAndName(UUID companyId, String name);
}
