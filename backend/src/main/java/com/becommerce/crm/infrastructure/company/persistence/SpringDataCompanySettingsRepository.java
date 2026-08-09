package com.becommerce.crm.infrastructure.company.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCompanySettingsRepository extends JpaRepository<CompanySettingsJpaEntity, UUID> {
    Optional<CompanySettingsJpaEntity> findByCompanyId(UUID companyId);
}
