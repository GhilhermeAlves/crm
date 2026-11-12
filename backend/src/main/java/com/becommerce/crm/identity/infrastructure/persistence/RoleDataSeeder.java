package com.becommerce.crm.identity.infrastructure.persistence;

import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
        // V133: companies tem RLS — lista os ids e lê cada empresa com o tenant dela.
        List<UUID> companyIds = companyRepository.findAllIds();
        if (companyIds.isEmpty()) {
            log.warn("Nenhuma empresa disponível para o seed de roles de sistema; pulando.");
            return;
        }

        int seeded = 0;
        for (UUID companyId : companyIds) {
            TenantContext.setCompanyId(companyId);
            try {
                boolean active = companyRepository.findById(companyId)
                        .map(Company::getStatus)
                        .map(status -> status.isActive())
                        .orElse(false);
                if (active) {
                    roleSeedService.seedRoles(companyId);
                    seeded++;
                }
            } catch (Exception e) {
                log.error("Erro ao seedar roles para empresa {}: {}", companyId, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Role seeding completed for {} companies", seeded);
    }
}