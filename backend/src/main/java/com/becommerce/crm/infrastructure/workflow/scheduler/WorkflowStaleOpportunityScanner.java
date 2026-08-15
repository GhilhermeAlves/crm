package com.becommerce.crm.infrastructure.workflow.scheduler;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Varredura periódica que detecta oportunidades "paradas" (Item 11, Sprint 14).
 *
 * <p>Todos os dias, para cada empresa, examina as oportunidades em aberto e
 * publica um {@link WorkflowTriggerEvent#opportunityStale} caso elas não tenham
 * recebido atividade há 7+ dias (mesmo critério do dashboard de atenção). O
 * evento é consumido pelos workflows ativos com trigger {@code OPPORTUNITY_STALE}.
 *
 * <p>O {@code eventId} determinístico (= {@code opportunityId}) garante
 * idempotência: uma ação configurada (ex.: criar tarefa de follow-up) dispara
 * no máximo uma vez por oportunidade, mesmo com varreduras diárias repetidas.
 *
 * <p>Fixa o {@code TenantContext} por empresa antes de ler os repositórios tenant-scoped
 * (RLS via GUC {@code app.current_company_id}) e o limpa ao final; {@code companies} não tem
 * RLS e é enumerada globalmente. O executor de workflows também assume o tenant por evento.
 */
@Component
public class WorkflowStaleOpportunityScanner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStaleOpportunityScanner.class);

    /** Uma oportunidade é considerada "parada" se não recebe atividade há 7+ dias. */
    static final long STALE_DAYS = 7L;

    private final CompanyRepository companyRepository;
    private final OpportunityRepository opportunityRepository;
    private final StageRepository stageRepository;
    private final ActivityRepository activityRepository;
    private final EventPublisher eventPublisher;

    public WorkflowStaleOpportunityScanner(CompanyRepository companyRepository,
                                           OpportunityRepository opportunityRepository,
                                           StageRepository stageRepository,
                                           ActivityRepository activityRepository,
                                           EventPublisher eventPublisher) {
        this.companyRepository = companyRepository;
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.activityRepository = activityRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${workflow.stale.cron:0 0 7 * * *}")
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        for (UUID companyId : companyRepository.findAll().stream().map(c -> c.getId()).toList()) {
            scanCompany(companyId, now);
        }
    }

    private void scanCompany(UUID companyId, LocalDateTime now) {
        TenantContext.setCompanyId(companyId);
        try {
            Map<UUID, String> stageNameById = stageRepository.findByCompanyId(companyId).stream()
                    .collect(Collectors.toMap(s -> s.getId(), s -> s.getName(), (a, b) -> a));
            opportunityRepository.findByCompanyId(companyId).stream()
                    .filter(o -> o.getStatus() == com.becommerce.crm.domain.pipeline.OpportunityStatus.OPEN)
                    .forEach(o -> {
                        long daysWithoutActivity = daysWithoutActivity(o.getId(), o.getCreatedAt(), now);
                        if (daysWithoutActivity >= STALE_DAYS) {
                            eventPublisher.publish(WorkflowTriggerEvent.opportunityStale(
                                    companyId,
                                    o.getId(),
                                    o.getContactId(),
                                    stageNameById.get(o.getStageId()),
                                    o.getValue(),
                                    daysWithoutActivity
                            ));
                        }
                    });
        } catch (Exception e) {
            log.error("Falha na varredura de oportunidades paradas para company={}: {}",
                    companyId, e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private long daysWithoutActivity(UUID opportunityId, LocalDateTime createdAt, LocalDateTime now) {
        LocalDateTime lastActivityAt = activityRepository
                .findLatestActivityAtByOpportunityId(opportunityId).orElse(createdAt);
        return Duration.between(lastActivityAt, now).toDays();
    }
}