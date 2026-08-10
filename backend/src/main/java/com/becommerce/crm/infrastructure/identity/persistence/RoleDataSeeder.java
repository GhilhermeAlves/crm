package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(1)
public class RoleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleDataSeeder.class);

    private final RoleSeedService roleSeedService;
    private final CompanyRepository companyRepository;

    @Value("${app.auth.provisioning.default-company-id:}")
    private String defaultCompanyId;

    public RoleDataSeeder(RoleSeedService roleSeedService,
                          CompanyRepository companyRepository) {
        this.roleSeedService = roleSeedService;
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(String... args) {
        UUID systemCompanyId = resolveSystemCompanyId();
        if (systemCompanyId == null) {
            log.warn("Nenhuma empresa disponível para o seed de roles de sistema; pulando.");
            return;
        }
        TenantContext.setCompanyId(systemCompanyId);
        try {
            roleSeedService.seedRoles(systemCompanyId);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID resolveSystemCompanyId() {
        if (defaultCompanyId != null && !defaultCompanyId.isBlank()) {
            try {
                return UUID.fromString(defaultCompanyId);
            } catch (IllegalArgumentException e) {
                log.warn("AUTH_DEFAULT_COMPANY_ID inválido ({}); usando primeira empresa ativa", defaultCompanyId);
            }
        }
        return companyRepository.findAll().stream()
                .filter(company -> company.getStatus() != null && company.getStatus().isActive())
                .findFirst()
                .map(Company::getId)
                .orElse(null);
    }
}