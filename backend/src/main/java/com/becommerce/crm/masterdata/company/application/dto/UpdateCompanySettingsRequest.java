package com.becommerce.crm.masterdata.company.application.dto;

public record UpdateCompanySettingsRequest(
        String timezone,
        String locale,
        String currency,
        String businessHours,
        String notificationPreferences,
        Boolean requireContactCpf
) {
}
