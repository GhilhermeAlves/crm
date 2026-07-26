package com.becommerce.crm.application.dashboard.dto;

import java.time.LocalDateTime;

public record RecentActivityResponse(
    String id,
    String userName,
    String action,
    String module,
    String description,
    LocalDateTime createdAt
) {}
