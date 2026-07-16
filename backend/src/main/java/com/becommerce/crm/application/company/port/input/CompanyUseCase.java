package com.becommerce.crm.application.company.port.input;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CompanySummaryResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.application.company.dto.UpdateCompanyRequest;

import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {
    CompanyResponse getCompanyById(UUID id);
    List<CompanySummaryResponse> listCompanies();
    CompanyResponse createCompany(CreateCompanyRequest request);
    CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request);
    void deleteCompany(UUID id);
}
