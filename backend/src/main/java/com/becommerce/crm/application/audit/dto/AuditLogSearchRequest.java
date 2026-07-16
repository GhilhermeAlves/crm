package com.becommerce.crm.application.audit.dto;

import java.time.LocalDateTime;

public record AuditLogSearchRequest(
    Integer page,
    Integer pageSize,
    String module,
    String action,
    String status,
    String userId,
    String entityId,
    String entityName,
    String search,
    LocalDateTime startDate,
    LocalDateTime endDate
) {
    public int getEffectivePage() { return page != null && page > 0 ? page : 1; }
    public int getEffectivePageSize() { return pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 20; }
}
