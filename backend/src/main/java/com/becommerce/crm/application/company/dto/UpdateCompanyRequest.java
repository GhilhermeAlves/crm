package com.becommerce.crm.application.company.dto;

public record UpdateCompanyRequest(
        String legalName,
        String tradingName,
        String email,
        String phone,
        String website,
        String addressZipCode,
        String addressStreet,
        String addressNumber,
        String addressComplement,
        String addressNeighborhood,
        String addressCity,
        String addressState,
        String addressCountry,
        String plan,
        String status,
        Integer maxUsers,
        Integer maxStorageMb,
        String logoUrl,
        String notes
) {
}
