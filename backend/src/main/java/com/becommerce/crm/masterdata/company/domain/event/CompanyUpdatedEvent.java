package com.becommerce.crm.masterdata.company.domain.event;

import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyUpdatedEvent(
        UUID companyId,
        String companyName,
        String cnpj,
        CompanyPlan plan,
        LocalDateTime occurredAt
) {
    public static CompanyUpdatedEvent create(Company company) {
        return new CompanyUpdatedEvent(
                company.getId(),
                company.getLegalName(),
                company.getCnpj(),
                company.getPlan(),
                LocalDateTime.now()
        );
    }
}
