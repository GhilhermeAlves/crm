package com.becommerce.crm.infrastructure.pipeline.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StageJpaRepository extends JpaRepository<StageJpaEntity, UUID> {

    List<StageJpaEntity> findByPipelineIdOrderByOrderNumAsc(UUID pipelineId);

    long countByPipelineId(UUID pipelineId);
}
