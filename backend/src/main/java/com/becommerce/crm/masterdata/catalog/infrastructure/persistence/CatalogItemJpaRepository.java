package com.becommerce.crm.masterdata.catalog.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CatalogItemJpaRepository extends JpaRepository<CatalogItemJpaEntity, UUID> {

    /** {@code pattern} já vem em minúsculas com {@code %...%} (ou nulo = sem busca textual). */
    @Query("SELECT i FROM CatalogItemJpaEntity i WHERE i.companyId = :companyId "
            + "AND (:itemType IS NULL OR i.itemType = :itemType) "
            + "AND (:active IS NULL OR i.active = :active) "
            + "AND (:pattern IS NULL OR LOWER(i.name) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.category, '')) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.description, '')) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.sku, '')) LIKE :pattern)")
    Page<CatalogItemJpaEntity> search(@Param("companyId") UUID companyId,
                                      @Param("itemType") String itemType,
                                      @Param("active") Boolean active,
                                      @Param("pattern") String pattern,
                                      Pageable pageable);

    @Query("SELECT COUNT(i) > 0 FROM CatalogItemJpaEntity i WHERE i.companyId = :companyId "
            + "AND i.sku = :sku AND (:excludeId IS NULL OR i.id <> :excludeId)")
    boolean existsBySku(@Param("companyId") UUID companyId, @Param("sku") String sku,
                        @Param("excludeId") UUID excludeId);
}
