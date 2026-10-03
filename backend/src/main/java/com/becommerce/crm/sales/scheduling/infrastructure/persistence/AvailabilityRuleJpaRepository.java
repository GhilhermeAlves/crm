package com.becommerce.crm.sales.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRuleJpaRepository extends JpaRepository<AvailabilityRuleJpaEntity, UUID> {

    List<AvailabilityRuleJpaEntity> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    void deleteByCompanyIdAndUserId(UUID companyId, UUID userId);
}
