package com.becommerce.crm.identity.me.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.me.application.dto.CompanyOptionResponse;
import com.becommerce.crm.identity.me.application.port.input.MeUseCase;
import com.becommerce.crm.identity.me.application.port.output.MyCompanyProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.membership.domain.exception.MembershipNotFoundException;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Company Switcher (Sprint 8.4). A empresa ativa autoritativa do sistema é
 * {@code users.company_id}; alterná-la (em transação) faz o próximo
 * {@code CurrentUser} ser resolvido com a nova empresa, o que propaga para
 * {@code TenantContext} / {@code TenantFilter} / RLS e para as roles/permissions.
 *
 * <p>Segurança: nunca confia no {@code companyId} do frontend. A empresa alvo só
 * é aceita se o usuário possui {@code memberships} {@code ACTIVE} nela — exceto
 * o dono do CRM (SUPER_ADMIN, V132), que lista e entra em todas as empresas sem
 * precisar de membership (e por isso não pode ser "desligado" por um cliente).
 */
@Service
public class MeService implements MeUseCase {

    private static final Logger log = LoggerFactory.getLogger(MeService.class);

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantAuditRecorder auditor;

    public MeService(MembershipRepository membershipRepository,
                     UserRepository userRepository,
                     CompanyRepository companyRepository,
                     UserRoleRepository userRoleRepository,
                     TenantAuditRecorder auditor) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyOptionResponse> listMyCompanies(UUID userId) {
        User user = requireUser(userId);
        if (userRoleRepository.isPlatformAdmin(userId)) {
            return companyRepository.findAll().stream()
                    .map(company -> toOption(company.getId(), company, company.getId().equals(user.getCompanyId())))
                    .sorted(Comparator.comparing(CompanyOptionResponse::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        return membershipRepository.findActiveCompanyOptionsByUserId(userId).stream()
                .map(option -> toOption(option, option.getCompanyId().equals(user.getCompanyId())))
                .toList();
    }

    @Override
    @Transactional
    public CompanyOptionResponse switchCompany(UUID userId, UUID companyId) {
        // Regra de segurança: a empresa alvo deve possuir membership ACTIVE do
        // usuário. Bloqueia empresa inexistente, sem membership e membership
        // inativa/removida (e usuário não autenticado é bloqueado no controller).
        boolean platformAdmin = userRoleRepository.isPlatformAdmin(userId);
        if (platformAdmin && companyRepository.findById(companyId).isEmpty()) {
            throw new CompanyNotFoundException(companyId);
        }
        if (!platformAdmin && !membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)) {
            throw new MembershipNotFoundException(
                    "Usuário sem membership ativa na empresa: " + companyId + ". Acesso ao CRM nesta empresa negado.");
        }
        User user = requireUser(userId);
        if (user.getCompanyId() != null && user.getCompanyId().equals(companyId)) {
            // Já é a empresa ativa — operação idempotente, retorna o item atual.
            return toOption(companyId, true);
        }

        // Espelha o OnboardingService: define o tenant alvo para o UPDATE da
        // própria linha do usuário sob RLS (bootstrap próprio por e-mail V025).
        try {
            TenantContext.setCompanyId(companyId);
            user.setCompanyId(companyId);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        log.info("Company Switcher: usuário {} ativou empresa {}", userId, companyId);

        // Auditoria de tenant (Sprint 8.6): troca da empresa ativa.
        auditor.record(companyId, AuditAction.UPDATE, AuditModule.TENANTS, "Company",
                companyId.toString(),
                "Company Switcher: usuário ativou a empresa",
                userId, null);
        return toOption(companyId, true);
    }

    private CompanyOptionResponse toOption(MyCompanyProjection projection, boolean active) {
        return new CompanyOptionResponse(projection.getCompanyId(), projection.getCompanyName(),
                projection.getLogoUrl(), active);
    }

    private CompanyOptionResponse toOption(UUID companyId, boolean active) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        return toOption(companyId, company, active);
    }

    private CompanyOptionResponse toOption(UUID companyId, Company company, boolean active) {
        String name = company.getTradingName() != null && !company.getTradingName().isBlank()
                ? company.getTradingName()
                : company.getLegalName();
        return new CompanyOptionResponse(companyId, name, company.getLogoUrl(), active);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MembershipNotFoundException("Usuário não encontrado: " + userId));
    }
}