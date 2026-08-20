package com.becommerce.crm.infrastructure.pipeline.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OpportunityHistoryJpaRepository extends JpaRepository<OpportunityHistoryJpaEntity, UUID> {

    List<OpportunityHistoryJpaEntity> findByOpportunityIdOrderByChangedAtAsc(UUID opportunityId);

    List<OpportunityHistoryJpaEntity> findByOpportunityIdInOrderByChangedAtAsc(Collection<UUID> opportunityIds);
}
