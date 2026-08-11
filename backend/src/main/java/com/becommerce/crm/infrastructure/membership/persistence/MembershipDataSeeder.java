package com.becommerce.crm.infrastructure.membership.persistence;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Backfill RLS-safe de memberships (Sprint 8.2). O backfill via migração V030
 * é best-effort (vira no-op quando a migração roda como usuário NOBYPASSRLS
 * sem contexto de tenant). Este seeder garante que TODO usuário existente
 * ganhe sua membership ativa, iterando empresa por empresa com
 * {@link TenantContext} definido (mesmo padrão do RoleDataSeeder).
 *
 * <p>Papel: prioridade SUPER_ADMIN &gt; OWNER &gt; ADMIN &gt; MANAGER &gt; AGENT
 * &gt; VIEWER (maior privilégio); sem roles → AGENT. Idempotente.
 */
@Component
@Order(2)
public class MembershipDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MembershipDataSeeder.class);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;

    public MembershipDataSeeder(CompanyRepository companyRepository,
                                UserRepository userRepository,
                                UserRoleRepository userRoleRepository,
                                RoleRepository roleRepository,
                                MembershipRepository membershipRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public void run(String... args) {
        List<Company> companies = companyRepository.findAll();
        if (companies.isEmpty()) {
            log.info("Membership seed: nenhuma empresa disponível; pulando.");
            return;
        }
        int created = 0;
        for (Company company : companies) {
            created += seedCompany(company.getId());
        }
        log.info("Membership seeding completed: {} membership(s) criada(s) em {} empresa(s)", created, companies.size());
    }

    private int seedCompany(UUID companyId) {
        TenantContext.setCompanyId(companyId);
        try {
            Map<UUID, String> roleNameByRoleId = resolveRoleNames(companyId);
            int created = 0;
            for (var user : userRepository.findAllByCompanyId(companyId)) {
                if (membershipRepository.existsActiveByUserIdAndCompanyId(user.getId(), companyId)) {
                    continue;
                }
                String role = resolveMembershipRole(user.getId(), companyId, roleNameByRoleId);
                try {
                    membershipRepository.save(Membership.activate(user.getId(), companyId, role));
                    created++;
                } catch (DataIntegrityViolationException e) {
                    // Corrida com o provisionamento/outra instância: membership já criada.
                }
            }
            return created;
        } finally {
            TenantContext.clear();
        }
    }

    private Map<UUID, String> resolveRoleNames(UUID companyId) {
        Map<UUID, String> roleNameByRoleId = new java.util.HashMap<>();
        for (Role role : roleRepository.findAllByCompanyId(companyId)) {
            roleNameByRoleId.put(role.getId(), role.getName());
        }
        return roleNameByRoleId;
    }

    private String resolveMembershipRole(UUID userId, UUID companyId, Map<UUID, String> roleNameByRoleId) {
        return userRoleRepository.findByUserIdAndCompanyId(userId, companyId).stream()
                .map(UserRole::getRoleId)
                .map(roleNameByRoleId::get)
                .filter(name -> name != null)
                .max(Comparator.comparingInt(this::rolePriority))
                .orElse(RoleName.AGENT.name());
    }

    private int rolePriority(String roleName) {
        return switch (roleName) {
            case "SUPER_ADMIN" -> 6;
            case "OWNER" -> 5;
            case "ADMIN" -> 4;
            case "MANAGER" -> 3;
            case "AGENT" -> 2;
            default -> 1;
        };
    }
}
