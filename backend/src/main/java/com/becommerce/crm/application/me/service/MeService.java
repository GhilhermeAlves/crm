package com.becommerce.crm.application.me.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.me.dto.CompanyOptionResponse;
import com.becommerce.crm.application.me.port.input.MeUseCase;
import com.becommerce.crm.application.me.port.output.MyCompanyProjection;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyNotFoundException;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.membership.exception.MembershipNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Company Switcher (Sprint 8.4). A empresa ativa autoritativa do sistema é
 * {@code users.company_id}; alterná-la (em transação) faz o próximo
 * {@code CurrentUser} ser resolvido com a nova empresa, o que propaga para
 * {@code TenantContext} / {@code TenantFilter} / RLS e para as roles/permissions.
 *
 * <p>Segurança: nunca confia no {@code companyId} do frontend. A empresa alvo só
 * é aceita se o usuário possui {@code memberships} {@code ACTIVE} nela.
 */
@Service
public class MeService implements MeUseCase {

    private static final Logger log = LoggerFactory.getLogger(MeService.class);

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public MeService(MembershipRepository membershipRepository,
                     UserRepository userRepository,
                     CompanyRepository companyRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyOptionResponse> listMyCompanies(UUID userId) {
        User user = requireUser(userId);
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
        if (!membershipRepository.existsActiveByUserIdAndCompanyId(userId, companyId)) {
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
        return toOption(companyId, true);
    }

    private CompanyOptionResponse toOption(MyCompanyProjection projection, boolean active) {
        return new CompanyOptionResponse(projection.getCompanyId(), projection.getCompanyName(),
                projection.getLogoUrl(), active);
    }

    private CompanyOptionResponse toOption(UUID companyId, boolean active) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
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