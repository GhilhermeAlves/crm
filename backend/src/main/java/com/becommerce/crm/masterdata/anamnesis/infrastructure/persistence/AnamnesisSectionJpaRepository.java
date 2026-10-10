package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnamnesisSectionJpaRepository extends JpaRepository<AnamnesisSectionJpaEntity, UUID> {

    List<AnamnesisSectionJpaEntity> findByModelIdOrderBySortOrderAsc(UUID modelId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AnamnesisSectionJpaEntity s WHERE s.modelId = :modelId")
    int deleteByModelId(@Param("modelId") UUID modelId);
}
