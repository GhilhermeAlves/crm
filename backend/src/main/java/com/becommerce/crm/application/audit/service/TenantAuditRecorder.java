package com.becommerce.crm.application.audit.service;

import com.becommerce.crm.application.audit.service.AuditService;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditLog;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.infrastructure.audit.context.AuditContext;
import com.becommerce.crm.infrastructure.audit.context.AuditContext.AuditContextData;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Reutilizador de auditoria de tenant (Sprint 8.6). Centraliza a escrita de
 * eventos de auditoria reutilizando a infraestrutura existente
 * ({@code audit_logs}, {@link AuditService}) — sem duplicar infraestrutura.
 *
 * <p>Enriquece o log com o ator/IP/user-agent capturados pelo
 * {@code AuditInterceptor} quando a ação acontece em uma requisição autenticada
 * (padrão do {@code AuditEventListener}); {@code actorUserId} é usado apenas como
 * fallback quando o contexto HTTP não está disponível (ex.: aceite/recusa por
 * token em fluxo parcialmente anônimo).
 */
@Component
public class TenantAuditRecorder {

    private final AuditService auditService;

    public TenantAuditRecorder(AuditService auditService) {
        this.auditService = auditService;
    }

    public void record(UUID companyId, AuditAction action, AuditModule module,
                       String entityName, String entityId, String description,
                       UUID actorUserId, Map<String, Object> newValues) {
        AuditLog auditLog = AuditLog.create(companyId, action, module);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);
        if (newValues != null) {
            auditLog.setNewValues(newValues);
        }

        AuditContextData context = AuditContext.get();
        if (context != null) {
            auditLog.setUserId(context.userId());
            auditLog.setUserName(context.userName());
            auditLog.setUserEmail(context.userEmail());
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
        } else if (actorUserId != null) {
            auditLog.setUserId(actorUserId);
        }

        recordWithTenantContext(companyId, auditLog);
    }

    private void recordWithTenantContext(UUID companyId, AuditLog auditLog) {
        UUID previous = TenantContext.getCompanyId();
        try {
            // Só define o tenant se ainda não estiver definido (preserva o
            // contexto da chamada atual, restaurando em finally).
            if (previous == null && companyId != null) {
                TenantContext.setCompanyId(companyId);
            }
            auditService.recordAudit(auditLog);
        } finally {
            if (previous == null) {
                TenantContext.clear();
            }
        }
    }
}