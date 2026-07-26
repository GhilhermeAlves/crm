package com.becommerce.crm.application.audit.port.output;

import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditLog;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.audit.AuditStatus;

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

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndCreatedAtAfter(UUID companyId, LocalDateTime since);

    List<AuditLog> findRecentByCompanyId(UUID companyId, int limit);
}
