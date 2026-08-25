package com.becommerce.crm.application.campaign.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.campaign.dto.ExecutionResponse;
import com.becommerce.crm.application.campaign.port.output.AudienceResolver;
import com.becommerce.crm.application.campaign.port.output.CampaignChannelDispatcher;
import com.becommerce.crm.application.campaign.port.output.CampaignEventRepository;
import com.becommerce.crm.application.campaign.port.output.CampaignExecutionRepository;
import com.becommerce.crm.application.campaign.port.output.CampaignRepository;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.application.template.port.output.TemplateRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.campaign.Campaign;
import com.becommerce.crm.domain.campaign.CampaignChannel;
import com.becommerce.crm.domain.campaign.CampaignExecution;
import com.becommerce.crm.domain.campaign.CampaignMessageEvent;
import com.becommerce.crm.domain.campaign.MessageEventStatus;
import com.becommerce.crm.domain.campaign.exception.CampaignNotFoundException;
import com.becommerce.crm.domain.template.MessageTemplate;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Motor de execução de campanhas (Sprint 17): batch + cursor persistido, sem
 * fila externa (RabbitMQ permanece débito — PLAN.md seção 13).
 *
 * Idempotência:
 * <ul>
 *   <li>claim atômico SCHEDULED -&gt; RUNNING no repositório (UPDATE condicional);</li>
 *   <li>UNIQUE (execution_id, recipient_id) impede evento duplicado por destinatário.</li>
 * </ul>
 * O worker roda em executor dedicado e restaura o {@link TenantContext} por lote.
 */
@Service
public class CampaignExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CampaignExecutionService.class);
    private static final int BATCH_SIZE = 20;

    private final CampaignRepository campaignRepository;
    private final CampaignExecutionRepository executionRepository;
    private final CampaignEventRepository eventRepository;
    private final AudienceResolver audienceResolver;
    private final TemplateRepository templateRepository;
    private final OmnichannelChannelRepository channelRepository;
    private final List<CampaignChannelDispatcher> dispatchers;
    private final TenantAuditRecorder auditor;
    private final com.becommerce.crm.application.identity.port.output.EventPublisher eventPublisher;
    private final long throttleMs;
    private final ExecutorService dispatchExecutor;

    public CampaignExecutionService(CampaignRepository campaignRepository,
                                    CampaignExecutionRepository executionRepository,
                                    CampaignEventRepository eventRepository,
                                    AudienceResolver audienceResolver,
                                    TemplateRepository templateRepository,
                                    OmnichannelChannelRepository channelRepository,
                                    List<CampaignChannelDispatcher> dispatchers,
                                    TenantAuditRecorder auditor,
                                    com.becommerce.crm.application.identity.port.output.EventPublisher eventPublisher,
                                    @Value("${campaign.dispatch.throttle-ms:200}") long throttleMs) {
        this.campaignRepository = campaignRepository;
        this.executionRepository = executionRepository;
        this.eventRepository = eventRepository;
        this.audienceResolver = audienceResolver;
        this.templateRepository = templateRepository;
        this.channelRepository = channelRepository;
        this.dispatchers = dispatchers;
        this.auditor = auditor;
        this.eventPublisher = eventPublisher;
        this.throttleMs = throttleMs;
        this.dispatchExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "campaign-dispatch");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdownExecutor() {
        dispatchExecutor.shutdownNow();
    }

    /**
     * Inicia a execução de uma campanha SCHEDULED. Idempotente: o claim
     * atômico falha se a campanha já estiver em execução/concluída.
     */
    public ExecutionResponse startExecution(UUID companyId, UUID campaignId, UUID actorUserId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() != com.becommerce.crm.domain.campaign.CampaignStatus.SCHEDULED) {
                throw new IllegalStateException(
                        "Somente campanhas SCHEDULED podem ser executadas (status atual: "
                                + campaign.getStatus() + ").");
            }
            if (!campaignRepository.claimForExecution(campaignId)) {
                throw new IllegalStateException("Campanha já está sendo executada.");
            }

            CampaignChannel channel = eventRepository.findChannelByCampaignId(campaignId)
                    .orElseThrow(() -> new IllegalStateException("Campanha sem canal configurado."));
            MessageTemplate template = templateRepository.findById(channel.getTemplateId())
                    .filter(t -> t.getCompanyId().equals(companyId))
                    .orElseThrow(() -> new IllegalStateException("Template da campanha não encontrado."));

            List<AudienceResolver.Recipient> recipients = audienceResolver.resolve(
                    companyId, campaign.getAudienceType(), campaign.getAudienceCriteria());
            if (recipients.isEmpty()) {
                // libera o claim para nova tentativa após ajuste do público
                campaignRepository.resetToScheduled(campaignId);
                throw new IllegalStateException("Público da campanha está vazio.");
            }

            String snapshot = "v" + channel.getTemplateVersion() + ":" + template.getBody();
            CampaignExecution execution = CampaignExecution.start(
                    companyId, campaignId, snapshot, recipients.size());
            CampaignExecution savedExecution = executionRepository.save(execution);

            List<CampaignMessageEvent> events = recipients.stream()
                    .map(r -> CampaignMessageEvent.createPending(companyId, savedExecution.getId(),
                            campaignId, r.id(), r.type(), r.phone()))
                    .toList();
            eventRepository.insertAllIgnoringConflicts(events);

            auditor.record(companyId, AuditAction.CUSTOM, com.becommerce.crm.domain.audit.AuditModule.CAMPAIGNS,
                    "Campaign", campaignId.toString(),
                    "Execução iniciada (" + recipients.size() + " destinatários)", actorUserId,
                    Map.of("executionId", savedExecution.getId().toString()));

            UUID executionId = savedExecution.getId();
            dispatchExecutor.submit(() -> processEvents(companyId, executionId));
            return toResponse(savedExecution);
        } finally {
            TenantContext.clear();
        }
    }

    /** Processa lotes PENDING até esgotar, pausar ou cancelar. Roda fora da thread HTTP. */
    void processEvents(UUID companyId, UUID executionId) {
        log.info("Iniciando processamento de eventos company={} execution={}", companyId, executionId);
        boolean more = true;
        while (more) {
            Optional<CampaignExecution> maybeExecution;
            try {
                TenantContext.setCompanyId(companyId);
                maybeExecution = executionRepository.findById(executionId);
                if (maybeExecution.isEmpty() || !maybeExecution.get().isDispatchable()) {
                    break;
                }
                CampaignExecution execution = maybeExecution.get();

                List<CampaignMessageEvent> batch =
                        eventRepository.findPendingBatch(executionId, BATCH_SIZE);
                if (batch.isEmpty()) {
                    finishIfDone(companyId, execution);
                    break;
                }

                CampaignChannel channel =
                        eventRepository.findChannelByCampaignId(execution.getCampaignId()).orElse(null);
                if (channel == null) {
                    break;
                }
                var dispatcher = dispatchers.stream()
                        .filter(d -> d.supports(channel.getChannelType()))
                        .findFirst()
                        .orElseThrow(() -> new CampaignChannelDispatcher.DispatchException(
                                "Nenhum dispatcher disponível para o canal " + channel.getChannelType()));

                for (CampaignMessageEvent event : batch) {
                    // revalida status a cada evento: pausa/cancelamento param o loop rápido
                    CampaignExecution current = executionRepository.findById(executionId).orElse(null);
                    if (current == null || !current.isDispatchable()) {
                        return;
                    }
                    sendOne(companyId, channel.getProviderChannelId(), event);
                }
                // contadores sincronizados com o banco (eventos em retry não contam como processados)
                long processed = eventRepository.countNotPendingByExecution(executionId);
                long failedFinal = eventRepository.countByExecutionAndStatus(executionId, MessageEventStatus.FAILED);
                execution.syncCounters(processed, failedFinal);
                execution.advanceCursor(batch.size());
                executionRepository.save(execution);

                if (throttleMs > 0) {
                    Thread.sleep(throttleMs);
                }
                more = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Falha ao processar lote da execução {}: {}", executionId, e.getMessage(), e);
                break;
            } finally {
                TenantContext.clear();
            }
        }
        log.info("Processamento encerrado execution={}", executionId);
    }

    private void sendOne(UUID companyId, UUID providerChannelId, CampaignMessageEvent event) {
        event.recordAttempt();
        if (event.getRecipientPhone() == null || event.getRecipientPhone().isBlank()) {
            event.markFailed("Destinatário sem telefone válido");
            persist(event);
            return;
        }
        try {
            CampaignChannelDispatcher dispatcher = dispatchers.stream()
                    .filter(d -> d.supports("WHATSAPP"))
                    .findFirst()
                    .orElseThrow(() -> new CampaignChannelDispatcher.DispatchException(
                            "Dispatcher WhatsApp não configurado."));
            CampaignChannelDispatcher.SendResult result = dispatcher.send(
                    new CampaignChannelDispatcher.SendCommand(
                            companyId, providerChannelId, event.getRecipientPhone(), renderBodyFor(event)));
            event.markSent(result.externalMessageId());
        } catch (Exception e) {
            if (event.canRetry()) {
                event.backToPending(e.getMessage());
            } else {
                event.markFailed(e.getMessage());
            }
        }
        persist(event);
    }

    private String renderBodyFor(CampaignMessageEvent event) {
        // O corpo renderizado é derivado do snapshot da execução; variáveis de
        // contato já foram resolvidas na criação dos eventos nesta sprint.
        var execution = executionRepository.findById(event.getExecutionId());
        return execution.map(CampaignExecution::getTemplateSnapshot)
                .map(s -> s.startsWith("v") && s.contains(":") ? s.substring(s.indexOf(':') + 1) : s)
                .orElse("");
    }

    private void persist(CampaignMessageEvent event) {
        try {
            TenantContext.setCompanyId(event.getCompanyId());
            eventRepository.saveEvent(event);
        } finally {
            TenantContext.clear();
        }
    }

    private void finishIfDone(UUID companyId, CampaignExecution execution) {
        long pending = eventRepository.countByExecutionAndStatus(
                execution.getId(), MessageEventStatus.PENDING);
        if (pending == 0 && execution.isDispatchable()) {
            execution.complete();
            executionRepository.save(execution);
            campaignRepository.completeIfRunning(execution.getCampaignId());

            auditor.record(companyId, AuditAction.CUSTOM,
                    com.becommerce.crm.domain.audit.AuditModule.CAMPAIGNS, "Campaign",
                    execution.getCampaignId().toString(),
                    "Campanha concluída (falhas: " + execution.getFailedCount() + ")",
                    null, Map.of("executionId", execution.getId().toString()));

            // Sprint 18: dispara automações (workflows) de campanha concluída
            eventPublisher.publish(
                    com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent.campaignCompleted(
                            companyId, execution.getCampaignId(), execution.getId(),
                            execution.getFailedCount(), execution.getTotalRecipients()));
        }
    }

    public void pause(UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() != com.becommerce.crm.domain.campaign.CampaignStatus.RUNNING) {
                throw new IllegalStateException("Apenas campanhas RUNNING podem ser pausadas.");
            }
            campaign.transitionTo(com.becommerce.crm.domain.campaign.CampaignStatus.PAUSED);
            campaignRepository.save(campaign);
            executionRepository.findLatestByCampaignId(campaignId).ifPresent(e -> {
                if (e.isDispatchable()) {
                    e.pause();
                    executionRepository.save(e);
                }
            });
        } finally {
            TenantContext.clear();
        }
    }

    public void resume(UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            Campaign campaign = requireOwned(companyId, campaignId);
            if (campaign.getStatus() != com.becommerce.crm.domain.campaign.CampaignStatus.PAUSED) {
                throw new IllegalStateException("Apenas campanhas PAUSED podem ser retomadas.");
            }
            campaign.transitionTo(com.becommerce.crm.domain.campaign.CampaignStatus.RUNNING);
            campaignRepository.save(campaign);
            executionRepository.findLatestByCampaignId(campaignId).ifPresent(e -> {
                if (!e.isDispatchable() && !isTerminal(e.getStatus())) {
                    e.resume();
                    executionRepository.save(e);
                    UUID executionId = e.getId();
                    dispatchExecutor.submit(() -> processEvents(companyId, executionId));
                }
            });
        } finally {
            TenantContext.clear();
        }
    }

    public void cancel(UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, campaignId);
            executionRepository.findLatestByCampaignId(campaignId).ifPresent(e -> {
                if (!isTerminal(e.getStatus())) {
                    e.cancel();
                    executionRepository.save(e);
                    eventRepository.cancelPendingByExecution(e.getId());
                }
            });
        } finally {
            TenantContext.clear();
        }
    }

    public ExecutionResponse getExecution(UUID companyId, UUID campaignId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, campaignId);
            CampaignExecution execution = executionRepository.findLatestByCampaignId(campaignId)
                    .orElseThrow(() -> new IllegalStateException("Campanha ainda não foi executada."));
            return toResponse(execution);
        } finally {
            TenantContext.clear();
        }
    }

    private static boolean isTerminal(com.becommerce.crm.domain.campaign.ExecutionStatus status) {
        return status == com.becommerce.crm.domain.campaign.ExecutionStatus.COMPLETED
                || status == com.becommerce.crm.domain.campaign.ExecutionStatus.CANCELLED;
    }

    private Campaign requireOwned(UUID companyId, UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new CampaignNotFoundException(campaignId);
        }
        return campaign;
    }

    private static ExecutionResponse toResponse(CampaignExecution e) {
        return new ExecutionResponse(e.getId(), e.getCampaignId(), e.getStatus(),
                e.getTotalRecipients(), e.getProcessedCount(), e.getFailedCount(),
                e.getStartedAt(), e.getFinishedAt());
    }
}
