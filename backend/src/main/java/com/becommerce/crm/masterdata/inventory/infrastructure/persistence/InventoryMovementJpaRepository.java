package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InventoryMovementJpaRepository extends JpaRepository<InventoryMovementJpaEntity, UUID> {

    @Query("SELECT m FROM InventoryMovementJpaEntity m WHERE m.companyId = :companyId "
            + "AND m.catalogItemId = :catalogItemId")
    Page<InventoryMovementJpaEntity> findByCatalogItem(@Param("companyId") UUID companyId,
                                                       @Param("catalogItemId") UUID catalogItemId,
                                                       Pageable pageable);
}
