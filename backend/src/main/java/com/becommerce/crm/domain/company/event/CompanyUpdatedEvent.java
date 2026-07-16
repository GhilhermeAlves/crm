package com.becommerce.crm.domain.company.event;

import com.becommerce.crm.domain.company.Company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyUpdatedEvent(
        UUID companyId,
        String companyName,
        String cnpj,
        LocalDateTime occurredAt
) {
    public static CompanyUpdatedEvent create(Company company) {
        return new CompanyUpdatedEvent(
                company.getId(),
                company.getLegalName(),
                company.getCnpj(),
                LocalDateTime.now()
        );
    }
}
