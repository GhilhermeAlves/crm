package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnamnesisQuestionJpaRepository extends JpaRepository<AnamnesisQuestionJpaEntity, UUID> {

    List<AnamnesisQuestionJpaEntity> findByModelIdOrderBySortOrderAsc(UUID modelId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AnamnesisQuestionJpaEntity q WHERE q.modelId = :modelId")
    int deleteByModelId(@Param("modelId") UUID modelId);
}
