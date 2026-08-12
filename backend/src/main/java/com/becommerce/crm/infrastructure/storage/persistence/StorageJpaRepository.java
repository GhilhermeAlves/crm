package com.becommerce.crm.infrastructure.storage.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StorageJpaRepository extends JpaRepository<StorageJpaEntity, UUID> {

    @Query("select coalesce(sum(s.sizeBytes), 0) from StorageJpaEntity s where s.companyId = :companyId")
    Long sumSizeByCompanyId(@Param("companyId") UUID companyId);
}