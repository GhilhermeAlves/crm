package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Decisão de acesso ao CRM (Sprint 6). A autenticação no Keycloak (quem é você)
 * é pré-condição, mas NÃO concede acesso ao CRM. O acesso é decidido aqui:
 *
 * <pre>
 *   users.is_active = true
 *   AND users.crm_enabled = true      (concessão explícita — novo usuário = false)
 *   AND companies.status = ACTIVE     (empresa pode operar)
 * </pre>
 *
 * Qualquer falha → {@link CrmAccessDeniedException} (CRM_ACCESS_DENIED).
 * Não substitui RBAC: roles/permissions definem o que o usuário pode fazer
 * dentro do CRM; este serviço decide apenas se ele pode entrar.
 */
@Service
public class CrmAccessService {

    private final CompanyRepository companyRepository;

    public CrmAccessService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Lança {@link CrmAccessDeniedException} se o usuário não puder acessar o CRM.
     */
    public void assertCrmAccess(User user) {
        if (user == null) {
            throw new CrmAccessDeniedException("Usuário não encontrado no CRM: acesso negado.");
        }
        if (!user.isActive()) {
            throw new CrmAccessDeniedException("Usuário inativo: acesso ao CRM negado.");
        }
        if (!user.isCrmEnabled()) {
            throw new CrmAccessDeniedException(
                "Usuário sem acesso ao CRM (crm_enabled=false): conceda acesso explicitamente.");
        }

        Company company = companyRepository.findById(user.getCompanyId())
            .orElseThrow(() -> new CrmAccessDeniedException(
                "Empresa não encontrada: acesso ao CRM negado."));

        if (!company.getStatus().canOperate()) {
            throw new CrmAccessDeniedException(
                "Empresa " + company.getStatus() + ": acesso ao CRM negado.");
        }
    }

    /**
     * Retorna {@code true} se o usuário pode acessar o CRM (mesma regra de
     * {@link #assertCrmAccess}, sem lançar exceção).
     */
    public boolean hasCrmAccess(User user) {
        try {
            assertCrmAccess(user);
            return true;
        } catch (CrmAccessDeniedException e) {
            return false;
        }
    }
}
