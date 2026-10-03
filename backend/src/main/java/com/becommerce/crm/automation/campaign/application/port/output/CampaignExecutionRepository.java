package com.becommerce.crm.automation.campaign.application.port.output;

import com.becommerce.crm.automation.campaign.domain.CampaignExecution;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída para execuções ({@code campaign_executions}, V058). */
public interface CampaignExecutionRepository {

    CampaignExecution save(CampaignExecution execution);

    Optional<CampaignExecution> findById(UUID id);

    Optional<CampaignExecution> findLatestByCampaignId(UUID campaignId);
}
