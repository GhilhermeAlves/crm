package com.becommerce.crm.masterdata.company.application.port.output;

import com.becommerce.crm.masterdata.company.domain.CompanyModules;

import java.util.Optional;
import java.util.UUID;

/** Segmento e módulos habilitados por empresa (colunas de {@code companies}, V104). */
public interface CompanyModulesRepository {

    Optional<CompanyModules> findByCompanyId(UUID companyId);

    /** Atualiza segmento/módulos; retorna {@code false} se a empresa não existir. */
    boolean update(UUID companyId, CompanyModules modules);
}
