package com.becommerce.crm.masterdata.catalog.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemJpaRepository extends JpaRepository<CatalogItemJpaEntity, UUID> {

    @Modifying
    @Query("UPDATE CatalogItemJpaEntity i SET i.active = :active "
            + "WHERE i.companyId = :companyId AND i.itemType = 'SERVICE' AND i.category IN :categories")
    int setActiveByCategories(@Param("companyId") UUID companyId,
                              @Param("categories") Collection<String> categories,
                              @Param("active") boolean active);

    @Query("SELECT LOWER(i.name) FROM CatalogItemJpaEntity i WHERE i.companyId = :companyId")
    List<String> findLowerCaseNames(@Param("companyId") UUID companyId);

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

    @Query("SELECT i FROM CatalogItemJpaEntity i WHERE i.companyId = :companyId "
            + "AND i.itemType = 'PRODUCT' "
            + "AND (:active IS NULL OR i.active = :active) "
            + "AND (:lowStock IS NULL OR NOT (:lowStock = TRUE) "
            + "     OR (i.minStock > 0 AND i.quantity <= i.minStock)) "
            + "AND (:category IS NULL OR i.category = :category) "
            + "AND (:pattern IS NULL OR LOWER(i.name) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.category, '')) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.sku, '')) LIKE :pattern)")
    Page<CatalogItemJpaEntity> searchProducts(@Param("companyId") UUID companyId,
                                              @Param("category") String category,
                                              @Param("lowStock") Boolean lowStock,
                                              @Param("active") Boolean active,
                                              @Param("pattern") String pattern,
                                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CatalogItemJpaEntity i WHERE i.id = :id")
    Optional<CatalogItemJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
