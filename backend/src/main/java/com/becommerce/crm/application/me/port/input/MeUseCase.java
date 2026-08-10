package com.becommerce.crm.application.me.port.input;

import com.becommerce.crm.application.me.dto.CompanyOptionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso do "Company Switcher" (Sprint 8.4): listar as empresas do usuário
 * com membership ativa e alternar a empresa ativa corrente.
 */
public interface MeUseCase {

    List<CompanyOptionResponse> listMyCompanies(UUID userId);

    CompanyOptionResponse switchCompany(UUID userId, UUID companyId);
}