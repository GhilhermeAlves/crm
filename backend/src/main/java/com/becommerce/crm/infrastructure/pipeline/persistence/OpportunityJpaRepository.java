package com.becommerce.crm.infrastructure.pipeline.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OpportunityJpaRepository extends JpaRepository<OpportunityJpaEntity, UUID> {

    List<OpportunityJpaEntity> findByPipelineId(UUID pipelineId);

    List<OpportunityJpaEntity> findByPipelineIdAndStatus(UUID pipelineId, String status);
}
