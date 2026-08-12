package com.becommerce.crm.application.company.dto;

/**
 * Uso/quota da empresa (Sprint 8.6): usuários, contatos e armazenamento com os
 * limites do plano da empresa.
 */
public record CompanyUsageResponse(
        UsageItem users,
        UsageItem contacts,
        StorageUsage storage
) {
    public record UsageItem(int current, int limit) {}
    public record StorageUsage(long currentMb, int limitMb) {}
}