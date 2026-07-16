package com.becommerce.crm.application.identity.dto;

import java.util.List;

public record RoleResponse(
    String id,
    String name,
    String description,
    String companyId,
    boolean isSystem,
    boolean isActive,
    List<PermissionResponse> permissions,
    java.time.LocalDateTime createdAt,
    java.time.LocalDateTime updatedAt
) {}
