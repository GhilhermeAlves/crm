package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.port.output.FollowUpRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gerencia o desfecho de um follow-up cujo envio é executado de forma
 * ASSÍNCRONA pelo consumer de sender (Sprint 23).
 *
 * <p>O sender confirma o resultado e este handler aplica a máquina de estados
 * no banco: {@code markSent} (sucesso) ou retry com backoff / {@code FAILED}
 * terminal (falha do provider), preservando {@link FollowUp#MAX_ATTEMPTS} e o
 * backoff progressivo do Spring 22. Sempre idempotente (guards de status nas
 * operações atômicas do repositório).
 */
@Service
public class FollowUpSendOutcomeHandler {

    private static final Logger log = LoggerFactory.getLogger(FollowUpSendOutcomeHandler.class);

    private final FollowUpRepository followUpRepository;
    private final TenantAuditRecorder auditor;

    public FollowUpSendOutcomeHandler(FollowUpRepository followUpRepository, TenantAuditRecorder auditor) {
        this.followUpRepository = followUpRepository;
        this.auditor = auditor;
    }

    public void markSent(UUID companyId, UUID followUpId, String externalMessageId) {
        if (followUpRepository.markSent(companyId, followUpId, externalMessageId, LocalDateTime.now())) {
            audit(companyId, followUpId, AuditAction.UPDATE,
                    "Follow-up executado (mensagem enviada: " + externalMessageId + ")");
        }
    }

    /** Falha de envio: retry com backoff ou FAILED terminal após MAX_ATTEMPTS. */
    public void onSendFailed(UUID companyId, UUID followUpId, String error) {
        FollowUp followUp = followUpRepository.findById(followUpId).orElse(null);
        if (followUp == null || !followUp.getCompanyId().equals(companyId)) {
            log.warn("Follow-up não encontrado para desfecho de envio (company={}, followUpId={})",
                    companyId, followUpId);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int attempt = followUp.getAttempts() + 1;
        if (attempt >= FollowUp.MAX_ATTEMPTS) {
            if (followUpRepository.markFailedTerminal(companyId, followUpId, error, now)) {
                audit(companyId, followUpId, AuditAction.UPDATE,
                        "Follow-up falhou definitivamente após " + attempt + " tentativas");
            }
            return;
        }
        if (followUpRepository.scheduleRetry(companyId, followUpId, now.plus(backoff(attempt)), error, now)) {
            audit(companyId, followUpId, AuditAction.UPDATE,
                    "Follow-up reagendado após falha (tentativa " + attempt + ")");
        }
    }

    /** Backoff progressivo: 15min, 1h, 4h. */
    public static Duration backoff(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofMinutes(15);
            case 2 -> Duration.ofHours(1);
            default -> Duration.ofHours(4);
        };
    }

    private void audit(UUID companyId, UUID followUpId, AuditAction action, String description) {
        try {
            auditor.record(companyId, action, AuditModule.FOLLOWUPS, "FollowUp",
                    followUpId.toString(), description, null, null);
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar auditoria de follow-up (company={}, followUpId={}): {}",
                    companyId, followUpId, e.getMessage());
        }
    }
}