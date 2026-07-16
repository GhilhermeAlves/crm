package com.becommerce.crm.application.company.dto;

public record CompanySummaryResponse(
        String id,
        String legalName,
        String tradingName,
        String cnpj,
        String email,
        String phone,
        String status,
        String plan
) {
}
