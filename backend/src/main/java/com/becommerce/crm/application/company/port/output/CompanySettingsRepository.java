package com.becommerce.crm.application.company.port.output;

import com.becommerce.crm.domain.company.CompanySettings;

import java.util.Optional;
import java.util.UUID;

public interface CompanySettingsRepository {
    Optional<CompanySettings> findByCompanyId(UUID companyId);
    CompanySettings save(CompanySettings settings);
}
