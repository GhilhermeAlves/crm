package com.becommerce.crm.application.company.dto;

public record UpdateCompanySettingsRequest(
        String timezone,
        String locale,
        String currency,
        String businessHours,
        String notificationPreferences
) {
}
