package com.becommerce.crm.domain.company.event;

import com.becommerce.crm.domain.company.Company;

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
