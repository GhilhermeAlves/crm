package com.becommerce.crm.automation.campaign.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignExecutionJpaRepository extends JpaRepository<CampaignExecutionJpaEntity, UUID> {

    Optional<CampaignExecutionJpaEntity> findFirstByCampaignIdOrderByCreatedAtDesc(UUID campaignId);
}
