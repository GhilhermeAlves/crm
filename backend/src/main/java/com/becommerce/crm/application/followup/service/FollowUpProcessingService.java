package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.followup.event.FollowUpExecutionEvent;
import com.becommerce.crm.application.followup.port.output.FollowUpEventPublisher;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dispatcher de FollowUps (Sprint 23). Chamado pelo {@code FollowUpScheduler}
 * (thread sem contexto HTTP). Para cada candidato:
 * <ol>
 *   <li><b>Claim atômico</b> — {@code PENDING -> PROCESSING} (ou recuperação de
 *       PROCESSING órfão após crash). Um único worker/instância vence.</li>
 *   <li><b>Publish</b> — publica {@link FollowUpExecutionEvent} na fila
 *       {@code crm.followup.executor}; a validação (Human Takeover, staleness,
 *       conversa) e o envio efetivo passam a rodar no consumer assíncrono
 *       {@link FollowUpExecutionService}, tirando o envio via provider do path
 *       do scheduler.</li>
 * </ol>
 *
 * <p>Se o publish falhar, o follow-up permanece PROCESSING e será recuperado pelo
 * claim de PROCESSING órfão ({@link #STALE_PROCESSING_CUTOFF}).
 */
@Service
public class FollowUpProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpProcessingService.class);

    /** Um PROCESSING mais antigo que isso é tratado como órfão (recuperação de crash). */
    public static final Duration STALE_PROCESSING_CUTOFF = Duration.ofMinutes(15);

    private final FollowUpRepository followUpRepository;
    private final FollowUpEventPublisher eventPublisher;

    public FollowUpProcessingService(FollowUpRepository followUpRepository,
                                     FollowUpEventPublisher eventPublisher) {
        this.followUpRepository = followUpRepository;
        this.eventPublisher = eventPublisher;
    }

    public void dispatch(UUID companyId, UUID followUpId) {
        LocalDateTime now = LocalDateTime.now();
        TenantContext.setCompanyId(companyId);
        try {
            boolean claimed = followUpRepository.claim(companyId, followUpId, now, now.minus(STALE_PROCESSING_CUTOFF));
            if (!claimed) {
                return;
            }
            FollowUpExecutionEvent event = FollowUpExecutionEvent.of(companyId, followUpId);
            try {
                eventPublisher.publishExecution(event);
                log.debug("Follow-up enfileirado: {} (company={})", followUpId, companyId);
            } catch (Exception e) {
                // Sem transação ativa: publish direto. Em caso de falha, o claim de
                // PROCESSING órfão recupera o follow-up no próximo tick.
                log.warn("Falha ao enfileirar follow-up {} (company={}); será recuperado pelo scheduler: {}",
                        followUpId, companyId, e.getMessage());
            }
        } finally {
            TenantContext.clear();
        }
    }
}