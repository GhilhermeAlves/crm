package com.becommerce.crm.masterdata.company.domain.event;

import com.becommerce.crm.masterdata.company.domain.Company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyCreatedEvent(
        UUID companyId,
        String companyName,
        String cnpj,
        String email,
        LocalDateTime occurredAt
) {
    public static CompanyCreatedEvent create(Company company) {
        return new CompanyCreatedEvent(
                company.getId(),
                company.getLegalName(),
                company.getCnpj(),
                company.getEmail(),
                LocalDateTime.now()
        );
    }
}
