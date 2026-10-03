package com.becommerce.crm.analytics.audit.application.port.out;

import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditLog;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.analytics.audit.domain.AuditStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> search(UUID companyId, AuditAction action, AuditModule module,
                          AuditStatus status, UUID userId, String entityName,
                          String entityId, String search,
                          LocalDateTime startDate, LocalDateTime endDate,
                          int page, int pageSize);
    long countSearch(UUID companyId, AuditAction action, AuditModule module,
                     AuditStatus status, UUID userId, String entityName,
                     String entityId, String search,
                     LocalDateTime startDate, LocalDateTime endDate);
    AuditLog findById(UUID id);
}
