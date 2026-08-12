package com.becommerce.crm.application.company.port.input;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CompanySettingsResponse;
import com.becommerce.crm.application.company.dto.CompanySummaryResponse;
import com.becommerce.crm.application.company.dto.CompanyUsageResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.application.company.dto.UpdateCompanyRequest;
import com.becommerce.crm.application.company.dto.UpdateCompanySettingsRequest;

import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {
    CompanyResponse getCompanyById(UUID id, UUID requesterCompanyId, boolean isSuperAdmin);
    CompanyUsageResponse getCompanyUsage(UUID id, UUID requesterCompanyId, boolean isSuperAdmin);
    List<CompanySummaryResponse> listCompanies(UUID requesterCompanyId, boolean isSuperAdmin);
    CompanyResponse createCompany(CreateCompanyRequest request, UUID creatorUserId);
    CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request, UUID requesterCompanyId, boolean isSuperAdmin);
    void deleteCompany(UUID id, UUID requesterCompanyId, boolean isSuperAdmin);
    CompanySettingsResponse getCompanySettings(UUID companyId, UUID requesterCompanyId);
    CompanySettingsResponse updateCompanySettings(UUID companyId, UpdateCompanySettingsRequest request, UUID requesterCompanyId);
}
