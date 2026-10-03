package com.becommerce.crm.analytics.audit.infrastructure.aspect;

import com.becommerce.crm.analytics.audit.infrastructure.annotation.Auditable;
import com.becommerce.crm.analytics.audit.infrastructure.context.AuditContext;
import com.becommerce.crm.analytics.audit.infrastructure.context.AuditContext.AuditContextData;
import com.becommerce.crm.analytics.audit.application.service.AuditService;
import com.becommerce.crm.analytics.audit.domain.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogAspect {

    private final AuditService auditService;

    public AuditLogAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        AuditContextData context = AuditContext.get();
        if (context == null) {
            return joinPoint.proceed();
        }

        String entityId = auditable.entityId();
        String entityName = auditable.entityName();

        if (entityId.isEmpty() && joinPoint.getArgs().length > 0) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof String str && str.matches("^[0-9a-fA-F-]{36}$")) {
                    entityId = str;
                    break;
                }
            }
        }

        AuditLog auditLog = AuditLog.create(context.companyId(), auditable.action(), auditable.module());
        auditLog.setUserId(context.userId());
        auditLog.setUserName(context.userName());
        auditLog.setUserEmail(context.userEmail());
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(auditable.description());
        auditLog.setIpAddress(context.ipAddress());
        auditLog.setUserAgent(context.userAgent());

        try {
            Object result = joinPoint.proceed();

            if (result != null) {
                auditLog.setNewValues(java.util.Map.of("result", result.toString()));
            }

            auditService.recordAudit(auditLog);
            return result;

        } catch (Throwable ex) {
            auditLog.markFailed(ex.getMessage());
            auditService.recordAudit(auditLog);
            throw ex;
        }
    }
}
