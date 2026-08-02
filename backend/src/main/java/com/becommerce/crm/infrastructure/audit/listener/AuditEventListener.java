package com.becommerce.crm.infrastructure.audit.listener;

import com.becommerce.crm.application.audit.service.AuditService;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditLog;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.company.event.CompanyCreatedEvent;
import com.becommerce.crm.domain.company.event.CompanyDeletedEvent;
import com.becommerce.crm.domain.company.event.CompanyUpdatedEvent;
import com.becommerce.crm.domain.identity.event.*;
import com.becommerce.crm.infrastructure.audit.context.AuditContext;
import com.becommerce.crm.infrastructure.audit.context.AuditContext.AuditContextData;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventListener {

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @Async
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        AuditContextData context = AuditContext.get();
        UUID companyId = context != null ? context.companyId() : event.companyId();

        AuditLog auditLog = AuditLog.create(companyId, AuditAction.CREATE, AuditModule.USERS);
        auditLog.setUserId(event.userId());
        auditLog.setEntityName("User");
        auditLog.setEntityId(event.userId().toString());
        auditLog.setDescription("Usuário criado: " + event.email());
        auditLog.setNewValues(Map.of("email", event.email()));

        if (context != null) {
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
        }

        recordWithTenantContext(companyId, auditLog);
    }

    @Async
    @EventListener
    public void handlePasswordChanged(PasswordChangedEvent event) {
        AuditLog auditLog = AuditLog.create(event.companyId(), AuditAction.CHANGE_PASSWORD, AuditModule.AUTH);
        auditLog.setUserId(event.userId());
        auditLog.setEntityName("User");
        auditLog.setEntityId(event.userId().toString());
        auditLog.setDescription("Senha alterada com sucesso");

        AuditContextData context = AuditContext.get();
        if (context != null) {
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
        }

        recordWithTenantContext(event.companyId(), auditLog);
    }

    @Async
    @EventListener
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        AuditLog auditLog = AuditLog.create(event.companyId(), AuditAction.RESET_PASSWORD, AuditModule.AUTH);
        auditLog.setUserId(event.userId());
        auditLog.setEntityName("User");
        auditLog.setEntityId(event.userId().toString());
        auditLog.setDescription("Solicitação de redefinição de senha");

        recordWithTenantContext(event.companyId(), auditLog);
    }

    @Async
    @EventListener
    public void handleCompanyCreated(CompanyCreatedEvent event) {
        AuditContextData context = AuditContext.get();
        UUID companyId = context != null ? context.companyId() : event.companyId();

        AuditLog auditLog = AuditLog.create(companyId, AuditAction.CREATE, AuditModule.TENANTS);
        auditLog.setEntityName("Company");
        auditLog.setEntityId(event.companyId().toString());
        auditLog.setDescription("Empresa criada: " + event.companyName());
        auditLog.setNewValues(Map.of(
            "companyName", event.companyName(),
            "cnpj", event.cnpj(),
            "email", event.email()
        ));

        if (context != null) {
            auditLog.setUserId(context.userId());
            auditLog.setUserName(context.userName());
            auditLog.setUserEmail(context.userEmail());
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
        }

        recordWithTenantContext(companyId, auditLog);
    }

    @Async
    @EventListener
    public void handleCompanyUpdated(CompanyUpdatedEvent event) {
        AuditContextData context = AuditContext.get();
        UUID companyId = context != null ? context.companyId() : event.companyId();

        AuditLog auditLog = AuditLog.create(companyId, AuditAction.UPDATE, AuditModule.TENANTS);
        auditLog.setEntityName("Company");
        auditLog.setEntityId(event.companyId().toString());
        auditLog.setDescription("Empresa atualizada: " + event.companyName());
        auditLog.setNewValues(Map.of("companyName", event.companyName()));

        if (context != null) {
            auditLog.setUserId(context.userId());
            auditLog.setUserName(context.userName());
            auditLog.setUserEmail(context.userEmail());
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
        }

        recordWithTenantContext(companyId, auditLog);
    }

    @Async
    @EventListener
    public void handleCompanyDeleted(CompanyDeletedEvent event) {
        AuditContextData context = AuditContext.get();
        UUID companyId = context != null ? context.companyId() : event.companyId();

        AuditLog auditLog = AuditLog.create(companyId, AuditAction.DELETE, AuditModule.TENANTS);
        auditLog.setEntityName("Company");
        auditLog.setEntityId(event.companyId().toString());
        auditLog.setDescription("Empresa excluída: " + event.companyName());

        if (context != null) {
            auditLog.setUserId(context.userId());
            auditLog.setUserName(context.userName());
            auditLog.setUserEmail(context.userEmail());
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
        }

        recordWithTenantContext(companyId, auditLog);
    }

    private void recordWithTenantContext(UUID companyId, AuditLog auditLog) {
        try {
            if (companyId != null) {
                TenantContext.setCompanyId(companyId);
            }
            auditService.recordAudit(auditLog);
        } finally {
            TenantContext.clear();
        }
    }
}
