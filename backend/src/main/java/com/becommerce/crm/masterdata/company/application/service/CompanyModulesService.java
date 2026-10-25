package com.becommerce.crm.masterdata.company.application.service;

import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.company.application.dto.CompanyModulesResponse;
import com.becommerce.crm.masterdata.company.application.dto.UpdateCompanyModulesRequest;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyModulesRepository;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.CompanyModules;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.masterdata.company.domain.InvalidCompanyModulesException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Segmento e módulos por empresa (V104). Qualquer usuário lê os módulos da
 * própria empresa (menu e telas); só o CRM_ADMIN altera (garantido no
 * controller) ou lê de outra empresa.
 */
@Service
public class CompanyModulesService {

    private final CompanyModulesRepository repository;

    public CompanyModulesService(CompanyModulesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CompanyModulesResponse get(UUID companyId, UUID requesterCompanyId, boolean isCrmAdmin) {
        if (!isCrmAdmin && !companyId.equals(requesterCompanyId)) {
            throw new CrmAccessDeniedException("Você só pode acessar os módulos da sua própria empresa.");
        }
        CompanyModules modules = repository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        return toResponse(companyId, modules);
    }

    @Transactional
    public CompanyModulesResponse update(UUID companyId, UpdateCompanyModulesRequest request) {
        CompanySegment segment;
        try {
            segment = CompanySegment.valueOf(request.segment().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidCompanyModulesException("Segmento inválido: " + request.segment());
        }
        CompanyModules modules = new CompanyModules(segment, CompanyModules.parse(request.enabledModules()));
        if (!repository.update(companyId, modules)) {
            throw new CompanyNotFoundException(companyId);
        }
        return toResponse(companyId, modules);
    }

    /**
     * Usado pela guarda de módulos. Empresa sem registro não é bloqueada aqui:
     * o próprio endpoint responde 404/403 pelas regras de acesso dele.
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(UUID companyId, CompanyModule module) {
        return repository.findByCompanyId(companyId)
                .map(m -> m.isEnabled(module))
                .orElse(true);
    }

    private static CompanyModulesResponse toResponse(UUID companyId, CompanyModules modules) {
        return new CompanyModulesResponse(companyId.toString(), modules.segment().name(),
                modules.orderedModules().stream().map(Enum::name).toList());
    }
}
