package com.becommerce.crm.application.company.dto;

import java.time.LocalDateTime;

public record CompanySummaryResponse(
        String id,
        String legalName,
        String tradingName,
        String cnpj,
        String email,
        String phone,
        String status,
        String plan,
        int maxUsers,
        int maxContacts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}