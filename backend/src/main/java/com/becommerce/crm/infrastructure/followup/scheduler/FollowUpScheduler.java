package com.becommerce.crm.infrastructure.followup.scheduler;

import com.becommerce.crm.application.followup.service.FollowUpProcessingService;
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
 * Scheduler de FollowUps (Sprint 22/23). A cada tick busca follow-ups vencidos em
 * TODAS as empresas via SECURITY DEFINER {@code app.followup_scheduler_candidates}
 * (mesmo padrão da V062/V044) e delega para o {@link FollowUpProcessingService}
 * (claim atômico + publish do evento de execução na fila {@code crm.followup.executor}),
 * com o TenantContext da empresa. A idempotência/concorrência é garantida pelo
 * claim no repositório — múltiplas instâncias podem rodar em paralelo sem
 * duplicar execução.
 */
@Component
public class FollowUpScheduler {

    private static final Logger log = LoggerFactory.getLogger(FollowUpScheduler.class);
    private static final int MAX_PER_TICK = 20;

    private final FollowUpProcessingService processingService;
    private final NamedParameterJdbcTemplate jdbc;

    public FollowUpScheduler(FollowUpProcessingService processingService,
                             NamedParameterJdbcTemplate jdbc) {
        this.processingService = processingService;
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${followup.scheduler.interval-ms:30000}")
    public void runDueFollowUps() {
        List<SchedulerCandidate> due = jdbc.query(
                "SELECT followup_id, company_id FROM app.followup_scheduler_candidates(:limit)",
                Map.of("limit", MAX_PER_TICK),
                (rs, n) -> new SchedulerCandidate(
                        rs.getObject("followup_id", UUID.class),
                        rs.getObject("company_id", UUID.class)));
        for (SchedulerCandidate candidate : due) {
            try {
                processingService.dispatch(candidate.companyId(), candidate.followUpId());
                log.debug("Follow-up processado: {} (company={})", candidate.followUpId(), candidate.companyId());
            } catch (Exception e) {
                log.error("Falha ao processar follow-up agendado {}: {}",
                        candidate.followUpId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private record SchedulerCandidate(UUID followUpId, UUID companyId) {}
}