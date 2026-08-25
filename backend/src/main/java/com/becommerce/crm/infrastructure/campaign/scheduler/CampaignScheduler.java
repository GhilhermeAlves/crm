package com.becommerce.crm.infrastructure.campaign.scheduler;

import com.becommerce.crm.application.campaign.service.CampaignExecutionService;
import com.becommerce.crm.domain.campaign.Campaign;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler de campanhas agendadas (Sprint 17). A cada minuto procura campanhas
 * SCHEDULED vencidas e dispara a execução — idempotente pelo claim atômico no
 * repositório (UPDATE ... WHERE status='SCHEDULED'). Sem lógica de automação
 * (escopo Sprint 18).
 */
@Component
public class CampaignScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignScheduler.class);
    private static final int MAX_PER_TICK = 5;

    private final CampaignExecutionService executionService;
    private final com.becommerce.crm.application.campaign.port.output.CampaignRepository campaignRepository;

    public CampaignScheduler(CampaignExecutionService executionService,
                             com.becommerce.crm.application.campaign.port.output.CampaignRepository campaignRepository) {
        this.executionService = executionService;
        this.campaignRepository = campaignRepository;
    }

    @Scheduled(fixedDelayString = "${campaign.scheduler.interval-ms:60000}")
    public void runDueCampaigns() {
        List<Campaign> due = campaignRepository.findDueForExecution(LocalDateTime.now(), MAX_PER_TICK);
        for (Campaign campaign : due) {
            try {
                TenantContext.setCompanyId(campaign.getCompanyId());
                executionService.startExecution(campaign.getCompanyId(), campaign.getId(), null);
                log.info("Campanha agendada iniciada: {} (company={})",
                        campaign.getId(), campaign.getCompanyId());
            } catch (IllegalStateException e) {
                // já reivindicada por outra instância ou público vazio: ignora silenciosamente
                log.debug("Campanha {} não iniciada: {}", campaign.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Falha ao iniciar campanha agendada {}: {}", campaign.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
