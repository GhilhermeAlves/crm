package com.becommerce.crm.application.identity.dto;

import java.time.LocalDateTime;

public record PermissionResponse(
    String id,
    String name,
    String description,
    String module,
    String resource,
    String action,
    LocalDateTime createdAt
) {}
