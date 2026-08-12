package com.becommerce.crm.application.invitation.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.invitation.dto.CreateInvitationRequest;
import com.becommerce.crm.application.invitation.dto.InvitationResponse;
import com.becommerce.crm.application.invitation.port.input.InvitationUseCase;
import com.becommerce.crm.application.invitation.port.output.InvitationRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.application.notification.EmailSender;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyNotFoundException;
import com.becommerce.crm.domain.company.CompanyStatus;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.invitation.Invitation;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import com.becommerce.crm.domain.invitation.exception.InvitationNotFoundException;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.infrastructure.invitation.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Casos de uso de convites (Sprint 8.5).
 *
 * <p>Toda escrita ocorre em transação. O aceite/recusa primeiro resolve o
 * convite por token (policy RLS por token) e só então troca o tenant para a
 * empresa-alvo para criar a membership. ADMIN/OWNER administram convites;
 * SUPER_ADMIN e OWNER não são roles concedíveis por convite (OWNER é exclusivo
 * do onboarding).
 */
@Service
public class InvitationService implements InvitationUseCase {

    /** Roles permitidas por convite (whitelist). */
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER", "AGENT", "VIEWER");

    private final InvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmailSender emailSender;
    private final InvitationTokenContextHolder tokenContext;

    public InvitationService(InvitationRepository invitationRepository,
                             CompanyRepository companyRepository,
                             MembershipRepository membershipRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             EmailSender emailSender,
                             InvitationTokenContextHolder tokenContext) {
        this.invitationRepository = invitationRepository;
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.emailSender = emailSender;
        this.tokenContext = tokenContext;
    }

    @Override
    @Transactional
    public InvitationResponse create(UUID companyId, CreateInvitationRequest request, UUID invitedBy) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException("Empresa inativa: não é possível convidar.");
        }

        String role = request.role().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Papel inválido para convite: " + role);
        }
        String email = normalize(request.email());
        boolean alreadyPending = invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING).stream()
                .anyMatch(i -> i.getEmail().equalsIgnoreCase(email));
        if (alreadyPending) {
            throw new IllegalArgumentException("Já existe convite pendente para " + email);
        }

        TenantContext.setCompanyId(companyId);
        String token = InvitationTokenService.generateToken();
        Invitation invitation = invitationRepository.save(
                Invitation.create(companyId, email, role, InvitationTokenService.hash(token), invitedBy));

        String tokenUrl = buildTokenUrl(token);
        emailSender.sendInvitation(email, company.getTradingName(), role, tokenUrl);
        return toResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listByCompany(UUID companyId, InvitationStatus status) {
        TenantContext.setCompanyId(companyId);
        return invitationRepository.findByCompanyId(companyId, status).stream()
                .map(InvitationService::toResponse).toList();
    }

    @Override
    @Transactional
    public void revoke(UUID invitationId, UUID companyId) {
        TenantContext.setCompanyId(companyId);
        Invitation invitation = invitationRepository.findById(invitationId)
                .filter(i -> i.getCompanyId().equals(companyId))
                .orElseThrow(() -> new InvitationNotFoundException("Convite não encontrado: " + invitationId));
        invitation.revoke(); // declínio/revogação -> REVOKED
        invitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public InvitationResponse accept(String token, UUID userId) {
        String hash = InvitationTokenService.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            Invitation invitation = invitationRepository.findByTokenHash(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));

            if (!invitation.isPending()) {
                throw new IllegalStateException("Convite não está pendente: " + invitation.getStatus());
            }
            if (invitation.isExpired()) {
                invitation.markExpired();
                invitationRepository.save(invitation);
                throw new IllegalStateException("Convite expirado.");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new com.becommerce.crm.domain.identity.exception.UserNotFoundException());
            if (!matchesEmail(invitation, user)) {
                throw new IllegalArgumentException(
                        "Este convite é destinado ao e-mail " + invitation.getEmail() + ".");
            }

            TenantContext.setCompanyId(invitation.getCompanyId());
            if (membershipRepository.existsActiveByUserIdAndCompanyId(user.getId(), invitation.getCompanyId())) {
                throw new IllegalStateException("Você já é membro desta empresa.");
            }

            membershipRepository.save(Membership.activate(user.getId(), invitation.getCompanyId(), invitation.getRole()));
            assignRole(user, invitation);
            user.grantCrmAccess();
            userRepository.save(user);

            invitation.accept();
            InvitationResponse response = toResponse(invitationRepository.save(invitation));
            TenantContext.clear();
            return response;
        } finally {
            tokenContext.clear();
        }
    }

    @Override
    @Transactional
    public InvitationResponse decline(String token, UUID userId) {
        String hash = InvitationTokenService.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            Invitation invitation = invitationRepository.findByTokenHash(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            if (!invitation.isPending()) {
                throw new IllegalStateException("Convite não está pendente: " + invitation.getStatus());
            }
            if (invitation.isExpired()) {
                invitation.markExpired();
                invitationRepository.save(invitation);
                throw new IllegalStateException("Convite expirado.");
            }
            TenantContext.setCompanyId(invitation.getCompanyId());
            invitation.revoke(); // decline -> REVOKED
            InvitationResponse response = toResponse(invitationRepository.save(invitation));
            TenantContext.clear();
            return response;
        } finally {
            tokenContext.clear();
        }
    }

    /** Vincula o papel RBAC (role_permissions) correspondente ao do convite. */
    private void assignRole(User user, Invitation invitation) {
        roleRepository.findByNameAndCompanyId(invitation.getRole(), invitation.getCompanyId())
                .map(Role::getId)
                .filter(roleId -> !userRoleRepository.existsByUserIdAndRoleId(user.getId(), roleId))
                .ifPresent(roleId -> userRoleRepository.save(UserRole.assign(user.getId(), roleId, invitation.getCompanyId())));
    }

    private boolean matchesEmail(Invitation invitation, User user) {
        String userEmail = user.getEmail() == null ? "" : user.getEmail().value();
        return invitation.getEmail().equalsIgnoreCase(userEmail);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String buildTokenUrl(String token) {
        return "/invitations/accept?token=" + token;
    }

    private static InvitationResponse toResponse(Invitation i) {
        return new InvitationResponse(i.getId(), i.getCompanyId(), i.getEmail(), i.getRole(),
                i.getStatus(), i.getInvitedBy(), i.getExpiresAt(), i.getCreatedAt());
    }
}