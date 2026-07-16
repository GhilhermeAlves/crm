package com.becommerce.crm.infrastructure.audit.aspect;

import com.becommerce.crm.infrastructure.audit.annotation.Auditable;
import com.becommerce.crm.infrastructure.audit.context.AuditContext;
import com.becommerce.crm.infrastructure.audit.context.AuditContext.AuditContextData;
import com.becommerce.crm.application.audit.service.AuditService;
import com.becommerce.crm.domain.audit.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

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
