package com.becommerce.crm.infrastructure.campaign.scheduler;

import com.becommerce.crm.application.campaign.service.CampaignExecutionService;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduler de campanhas agendadas (Sprint 17). A cada minuto busca campanhas
 * SCHEDULED vencidas em TODAS as empresas via SECURITY DEFINER
 * {@code app.campaign_scheduler_candidates} (mesmo padrão da V044) e dispara a
 * execução com o TenantContext da empresa — idempotente pelo claim atômico no
 * repositório (UPDATE ... WHERE status='SCHEDULED'). Sem lógica de automação
 * (escopo Sprint 18).
 */
@Component
public class CampaignScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignScheduler.class);
    private static final int MAX_PER_TICK = 5;

    private final CampaignExecutionService executionService;
    private final NamedParameterJdbcTemplate jdbc;

    public CampaignScheduler(CampaignExecutionService executionService,
                             NamedParameterJdbcTemplate jdbc) {
        this.executionService = executionService;
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${campaign.scheduler.interval-ms:60000}")
    public void runDueCampaigns() {
        List<SchedulerCandidate> due = jdbc.query(
                "SELECT campaign_id, company_id FROM app.campaign_scheduler_candidates(:limit)",
                Map.of("limit", MAX_PER_TICK),
                (rs, n) -> new SchedulerCandidate(
                        rs.getObject("campaign_id", UUID.class),
                        rs.getObject("company_id", UUID.class)));
        for (SchedulerCandidate candidate : due) {
            try {
                TenantContext.setCompanyId(candidate.companyId());
                executionService.startExecution(candidate.companyId(), candidate.campaignId(), null);
                log.info("Campanha agendada iniciada: {} (company={})",
                        candidate.campaignId(), candidate.companyId());
            } catch (IllegalStateException e) {
                // já reivindicada por outra instância ou público vazio: ignora silenciosamente
                log.debug("Campanha {} não iniciada: {}", candidate.campaignId(), e.getMessage());
            } catch (Exception e) {
                log.error("Falha ao iniciar campanha agendada {}: {}",
                        candidate.campaignId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private record SchedulerCandidate(UUID campaignId, UUID companyId) {}
}
