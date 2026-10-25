package com.becommerce.crm.masterdata.company.application.port.input;

import com.becommerce.crm.masterdata.company.application.dto.CompanyResponse;
import com.becommerce.crm.masterdata.company.application.dto.CompanySettingsResponse;
import com.becommerce.crm.masterdata.company.application.dto.CompanySummaryResponse;
import com.becommerce.crm.masterdata.company.application.dto.CompanyUsageResponse;
import com.becommerce.crm.masterdata.company.application.dto.CreateCompanyRequest;
import com.becommerce.crm.masterdata.company.application.dto.UpdateCompanyRequest;
import com.becommerce.crm.masterdata.company.application.dto.UpdateCompanySettingsRequest;

import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {
    CompanyResponse getCompanyById(UUID id, UUID requesterCompanyId, boolean isCrmAdmin);
    CompanyUsageResponse getCompanyUsage(UUID id, UUID requesterCompanyId, boolean isCrmAdmin);
    List<CompanySummaryResponse> listCompanies(UUID requesterCompanyId, boolean isCrmAdmin);
    CompanyResponse createCompany(CreateCompanyRequest request, UUID creatorUserId);
    CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request, UUID requesterCompanyId, boolean isCrmAdmin);
    void deleteCompany(UUID id, UUID requesterCompanyId, boolean isCrmAdmin);
    CompanySettingsResponse getCompanySettings(UUID companyId, UUID requesterCompanyId);
    CompanySettingsResponse updateCompanySettings(UUID companyId, UpdateCompanySettingsRequest request, UUID requesterCompanyId);
}
