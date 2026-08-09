package com.becommerce.crm.application.company.dto;

import java.time.LocalDateTime;

public record CompanySettingsResponse(
        String companyId,
        String timezone,
        String locale,
        String currency,
        String businessHours,
        String notificationPreferences,
        LocalDateTime updatedAt
) {
}
