package com.becommerce.crm.application.company.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
        String id,
        String legalName,
        String tradingName,
        String cnpj,
        String stateRegistration,
        String municipalRegistration,
        String email,
        String phone,
        String website,
        AddressResponse address,
        String status,
        String plan,
        int maxUsers,
        int maxStorageMb,
        String logoUrl,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record AddressResponse(
            String zipCode,
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            String state,
            String country
    ) {}
}
