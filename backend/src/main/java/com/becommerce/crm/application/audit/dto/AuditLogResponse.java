package com.becommerce.crm.application.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    String id,
    String companyId,
    String userId,
    String userName,
    String userEmail,
    String action,
    String module,
    String entityName,
    String entityId,
    String description,
    Object oldValues,
    Object newValues,
    String ipAddress,
    String userAgent,
    String requestMethod,
    String requestUri,
    String status,
    boolean success,
    LocalDateTime createdAt
) {}
