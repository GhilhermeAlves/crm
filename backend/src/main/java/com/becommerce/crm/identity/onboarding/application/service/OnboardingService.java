package com.becommerce.crm.identity.onboarding.application.service;

import com.becommerce.crm.masterdata.company.application.dto.CompanyResponse;
import com.becommerce.crm.masterdata.company.application.port.input.CompanyUseCase;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.identity.onboarding.application.port.input.OnboardingUseCase;
import com.becommerce.crm.application.workflow.service.WorkflowTemplateSeeder;
import com.becommerce.crm.identity.infrastructure.persistence.RoleSeedService;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.UserRole;
import com.becommerce.crm.identity.domain.valueobject.RoleName;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.application.dto.CreateCompanyRequest;
import com.becommerce.crm.identity.domain.Role;
import com.becommerce.crm.masterdata.company.application.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Onboarding self-service de empresa (Sprint 8.3). Usuário provisionado SEM
 * empresa (company_id null) cria a primeira empresa e torna-se o OWNER.
 *
 * <p>Passos, dentro de uma transação:
 * <ol>
 *   <li>Cria a {@code companies} (tabela global, sem RLS).</li>
 *   <li>Define o {@link TenantContext} para a nova empresa — as policies RLS
 *       (V019/V030) exigem {@code company_id = app.current_tenant_id()} para
 *       INSERT em roles/membership.</li>
 *   <li>Seed dos papéis RBAC padrão (RoleSeedService).</li>
 *   <li>Cria a {@code memberships} do OWNER — o trigger V030
 *       (membership_sync_active_company) eleva {@code users.company_id}.</li>
 *   <li>Atribui o papel {@code ADMIN} (RBAC) ao dono e concede acesso ao CRM
 *       ({@code crm_enabled=true}).</li>
 * </ol>
 */
@Service
public class OnboardingService implements OnboardingUseCase {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final CompanyRepository companyRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleSeedService roleSeedService;
    private final CompanyUseCase companyUseCase;
    private final WorkflowTemplateSeeder workflowTemplateSeeder;

    public OnboardingService(CompanyRepository companyRepository,
                             MembershipRepository membershipRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             UserRepository userRepository,
                             RoleSeedService roleSeedService,
                             CompanyUseCase companyUseCase,
                             WorkflowTemplateSeeder workflowTemplateSeeder) {
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleSeedService = roleSeedService;
        this.companyUseCase = companyUseCase;
        this.workflowTemplateSeeder = workflowTemplateSeeder;
    }

    @Override
    @Transactional
    public CompanyResponse onboard(CreateCompanyRequest request, User owner) {
        String cnpj = request.cnpj() != null ? request.cnpj() : request.legalName().toLowerCase().replaceAll("[^a-z0-9]", "") + "00";
        String email = request.email() != null ? request.email() : owner.getEmail().value();

        if (companyRepository.existsByCnpj(cnpj)) {
            throw new IllegalStateException("Uma empresa com este CNPJ já existe.");
        }
        if (companyRepository.existsByEmail(email)) {
            throw new IllegalStateException("Uma empresa com este e-mail já existe.");
        }

        Company company = Company.create(
                request.legalName(),
                request.tradingName() != null ? request.tradingName() : request.legalName(),
                cnpj,
                request.stateRegistration(),
                request.municipalRegistration(),
                email,
                request.phone() != null ? request.phone() : "",
                request.website(),
                request.addressZipCode() != null ? request.addressZipCode() : "",
                request.addressStreet() != null ? request.addressStreet() : "",
                request.addressNumber() != null ? request.addressNumber() : "",
                request.addressComplement(),
                request.addressNeighborhood() != null ? request.addressNeighborhood() : "",
                request.addressCity() != null ? request.addressCity() : "",
                request.addressState() != null ? request.addressState() : "",
                request.addressCountry() != null ? request.addressCountry() : "Brasil",
                request.plan() != null ? CompanyPlan.valueOf(request.plan().toUpperCase()) : CompanyPlan.STARTER,
                CompanyService.DEFAULT_MAX_USERS,
                CompanyService.DEFAULT_MAX_STORAGE_MB,
                CompanyService.DEFAULT_MAX_CONTACTS,
                request.logoUrl(),
                request.notes());

        Company saved = companyRepository.save(company);

        try {
            TenantContext.setCompanyId(saved.getId());
            roleSeedService.seedRoles(saved.getId());
            workflowTemplateSeeder.seedTemplates(saved.getId());

            // Membership OWNER (fonte de verdade). O trigger V030 eleva
            // users.company_id para esta empresa (primeira membership ativa).
            if (!membershipRepository.existsActiveByUserIdAndCompanyId(owner.getId(), saved.getId())) {
                membershipRepository.save(Membership.activate(owner.getId(), saved.getId(), Membership.OWNER_ROLE));
            }

            // RBAC: o dono recebe o papel ADMIN (permite operar os módulos).
            Role admin = roleRepository.findByNameAndCompanyId(RoleName.ADMIN.name(), saved.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Papel ADMIN não encontrado após o seed da nova empresa."));
            if (!userRoleRepository.existsByUserIdAndRoleId(owner.getId(), admin.getId())) {
                userRoleRepository.save(UserRole.assign(owner.getId(), admin.getId(), saved.getId()));
            }

            // Concede acesso ao CRM e seta a empresa ativa explicitamente (idempotente
            // ao trigger e garantido sob RLS via bootstrap de identidade por e-mail V025).
            owner.grantCrmAccess();
            owner.setCompanyId(saved.getId());
            userRepository.save(owner);
        } finally {
            TenantContext.clear();
        }

        log.info("Onboarding: empresa {} criada (owner={})", saved.getId(), owner.getId());
        return companyUseCase.getCompanyById(saved.getId(), saved.getId(), false);
    }
}