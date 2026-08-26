package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class RoleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleDataSeeder.class);

    private final RoleSeedService roleSeedService;
    private final CompanyRepository companyRepository;

    public RoleDataSeeder(RoleSeedService roleSeedService,
                          CompanyRepository companyRepository) {
        this.roleSeedService = roleSeedService;
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(String... args) {
        List<Company> activeCompanies = companyRepository.findAll().stream()
                .filter(company -> company.getStatus() != null && company.getStatus().isActive())
                .toList();

        if (activeCompanies.isEmpty()) {
            log.warn("Nenhuma empresa disponível para o seed de roles de sistema; pulando.");
            return;
        }

        for (Company company : activeCompanies) {
            TenantContext.setCompanyId(company.getId());
            try {
                roleSeedService.seedRoles(company.getId());
            } catch (Exception e) {
                log.error("Erro ao seedar roles para empresa {}: {}", company.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Role seeding completed for {} companies", activeCompanies.size());
    }
}