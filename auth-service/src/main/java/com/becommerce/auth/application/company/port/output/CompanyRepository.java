package com.becommerce.auth.application.company.port.output;

import com.becommerce.auth.domain.company.CompanyStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para o status da empresa do usuário (tabela {@code companies},
 * schema de propriedade do crm-backend; somente leitura). Usada pelo gate de
 * acesso ao CRM (Sprint 6).
 */
public interface CompanyRepository {

    Optional<CompanyStatus> findStatusById(UUID companyId);
}
