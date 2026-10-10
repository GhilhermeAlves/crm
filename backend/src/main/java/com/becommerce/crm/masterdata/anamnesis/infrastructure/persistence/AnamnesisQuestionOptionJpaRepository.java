package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnamnesisQuestionOptionJpaRepository
        extends JpaRepository<AnamnesisQuestionOptionJpaEntity, UUID> {

    @Query("SELECT o FROM AnamnesisQuestionOptionJpaEntity o "
            + "WHERE o.questionId IN "
            + "  (SELECT q.id FROM AnamnesisQuestionJpaEntity q WHERE q.modelId = :modelId) "
            + "ORDER BY o.sortOrder ASC")
    List<AnamnesisQuestionOptionJpaEntity> findByModelIdOrderBySortOrderAsc(@Param("modelId") UUID modelId);
}
