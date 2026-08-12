package com.becommerce.crm.infrastructure.storage.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StorageJpaRepository extends JpaRepository<StorageJpaEntity, UUID> {

    Long sumSizeByCompanyId(UUID companyId);
}