package com.becommerce.crm.infrastructure.activity.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityJpaRepository extends JpaRepository<ActivityJpaEntity, UUID> {

    List<ActivityJpaEntity> findByCompanyId(UUID companyId);

    List<ActivityJpaEntity> findByContactId(UUID contactId);

    List<ActivityJpaEntity> findByOpportunityId(UUID opportunityId);

    List<ActivityJpaEntity> findByOpportunityIdIn(Collection<UUID> opportunityIds);

    List<ActivityJpaEntity> findByCompanyIdOrderByActivityAtDesc(UUID companyId, Pageable pageable);

    Optional<ActivityJpaEntity> findTopByOpportunityIdOrderByActivityAtDesc(UUID opportunityId);

    Optional<ActivityJpaEntity> findTopByContactIdOrderByActivityAtDesc(UUID contactId);
}