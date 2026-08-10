package com.becommerce.crm.application.onboarding.port.input;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.domain.identity.User;

/**
 * Onboarding self-service (Sprint 8.3): usuário sem empresa cria a primeira
 * empresa e torna-se o OWNER.
 */
public interface OnboardingUseCase {
    CompanyResponse onboard(CreateCompanyRequest request, User owner);
}