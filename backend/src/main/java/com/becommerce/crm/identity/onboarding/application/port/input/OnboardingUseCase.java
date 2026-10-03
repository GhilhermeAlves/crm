package com.becommerce.crm.identity.onboarding.application.port.input;

import com.becommerce.crm.masterdata.company.application.dto.CompanyResponse;
import com.becommerce.crm.masterdata.company.application.dto.CreateCompanyRequest;
import com.becommerce.crm.identity.domain.User;

/**
 * Onboarding self-service (Sprint 8.3): usuário sem empresa cria a primeira
 * empresa e torna-se o OWNER.
 */
public interface OnboardingUseCase {
    CompanyResponse onboard(CreateCompanyRequest request, User owner);
}