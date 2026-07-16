package com.becommerce.crm.domain.company.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyDeletedEvent(
        UUID companyId,
        String companyName,
        String cnpj,
        LocalDateTime occurredAt
) {
    public static CompanyDeletedEvent create(UUID companyId, String companyName, String cnpj) {
        return new CompanyDeletedEvent(
                companyId,
                companyName,
                cnpj,
                LocalDateTime.now()
        );
    }
}
