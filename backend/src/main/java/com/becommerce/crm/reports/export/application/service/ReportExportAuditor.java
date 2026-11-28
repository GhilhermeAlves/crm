package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.reports.export.domain.ReportExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Auditoria do ciclo de exportação, reutilizando {@code audit_logs} via
 * {@link TenantAuditRecorder} (módulo REPORTS, entidade {@code report_export}).
 *
 * <p>Só grava metadados: ids, tipo, formato, período, linhas e duração — nunca
 * termos de busca nem dados pessoais. Roda em transação própria
 * (REQUIRES_NEW) para que o registro de um acesso negado sobreviva ao rollback
 * do fluxo principal e uma falha aqui não contamine a transação de quem chamou;
 * qualquer erro vira só um log de aviso.
 */
@Component
public class ReportExportAuditor {

    static final String ENTITY = "report_export";

    public enum Event { REQUESTED, READY, FAILED, CANCELED, EXPIRED, DOWNLOADED, ACCESS_DENIED }

    private static final Logger log = LoggerFactory.getLogger(ReportExportAuditor.class);

    private final TenantAuditRecorder recorder;
    private final TransactionTemplate tx;

    public ReportExportAuditor(TenantAuditRecorder recorder, PlatformTransactionManager transactionManager) {
        this.recorder = recorder;
        this.tx = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        if (tx != null) {
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    /** Evento sobre a exportação; o ator é o autor do pedido. */
    public void record(Event event, ReportExport export) {
        record(event, export, export.requestedBy());
    }

    /** Evento com ator explícito (ex.: acesso negado a outro usuário). */
    public void record(Event event, ReportExport export, UUID actorUserId) {
        try {
            Map<String, Object> values = values(event, export);
            String description = "Exportação de relatório: " + event.name();
            Runnable write = () -> recorder.record(export.companyId(), AuditAction.EXPORT, AuditModule.REPORTS,
                    ENTITY, export.id().toString(), description, actorUserId, values);
            if (tx == null) {
                write.run();
            } else {
                tx.executeWithoutResult(status -> write.run());
            }
        } catch (RuntimeException ex) {
            log.warn("[RELATORIOS] falha ao auditar {} da exportação {}: {}", event, export.id(),
                    ex.getClass().getSimpleName());
        }
    }

    static Map<String, Object> values(Event event, ReportExport export) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("event", event.name());
        values.put("exportId", export.id().toString());
        values.put("type", export.type().name());
        values.put("format", export.format().name());
        values.put("periodFrom", String.valueOf(export.periodFrom()));
        values.put("periodTo", String.valueOf(export.periodTo()));
        values.put("status", export.status().name());
        if (export.rowCount() != null) {
            values.put("rowCount", export.rowCount());
        }
        if (export.finishedAt() != null && export.createdAt() != null
                && (event == Event.READY || event == Event.FAILED || event == Event.CANCELED)) {
            values.put("durationMs", Duration.between(export.createdAt(), export.finishedAt()).toMillis());
        }
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            values.put("traceId", traceId);
        }
        return values;
    }
}
