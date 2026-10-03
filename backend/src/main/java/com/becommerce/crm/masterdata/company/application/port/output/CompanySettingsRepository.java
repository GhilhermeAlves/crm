package com.becommerce.crm.masterdata.company.application.port.output;

import com.becommerce.crm.masterdata.company.domain.CompanySettings;

import java.util.Optional;
import java.util.UUID;

public interface CompanySettingsRepository {
    Optional<CompanySettings> findByCompanyId(UUID companyId);
    CompanySettings save(CompanySettings settings);
}
