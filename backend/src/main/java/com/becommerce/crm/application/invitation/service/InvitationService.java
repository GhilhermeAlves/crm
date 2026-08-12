package com.becommerce.crm.application.invitation.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
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
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.invitation.Invitation;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import com.becommerce.crm.domain.invitation.exception.InvitationNotFoundException;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import com.becommerce.crm.infrastructure.invitation.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.infrastructure.invitation.rate.InvitationRateLimiter;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final InvitationRateLimiter rateLimiter;
    private final TenantAuditRecorder auditor;

    private final String invitationBaseUrl;

    public InvitationService(InvitationRepository invitationRepository,
                             CompanyRepository companyRepository,
                             MembershipRepository membershipRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             EmailSender emailSender,
                             InvitationTokenContextHolder tokenContext,
                             InvitationRateLimiter rateLimiter,
                             TenantAuditRecorder auditor,
                             @org.springframework.beans.factory.annotation.Value("${app.invitations.base-url:}") String invitationBaseUrl) {
        this.invitationRepository = invitationRepository;
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.emailSender = emailSender;
        this.tokenContext = tokenContext;
        this.rateLimiter = rateLimiter;
        this.auditor = auditor;
        this.invitationBaseUrl = invitationBaseUrl;
    }

    @Override
    @Transactional
    public InvitationResponse create(UUID companyId, CreateInvitationRequest request, UUID invitedBy) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalStateException("Empresa inativa: não é possível convidar.");
        }
        if (!rateLimiter.tryCreate(companyId.toString())) {
            throw new IllegalStateException("Excesso de convites na janela atual. Tente novamente mais tarde.");
        }

        String role = request.role().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Papel inválido para convite: " + role);
        }
        String email = normalize(request.email());

        // Enforcement max_users (Sprint 8.6): membros ativos + convites
        // pendentes não podem exceder o limite do plano. Roda sob o tenant da
        // empresa-alvo (RLS) para contagem correta e sem bypass por company_id.
        TenantContext.setCompanyId(companyId);
        long activeUsers = membershipRepository.countActiveByCompanyId(companyId);
        long pendingInvites = invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING).size();
        if (activeUsers + pendingInvites >= company.getMaxUsers()) {
            throw new QuotaExceededException(
                    "Limite de usuários da empresa atingido (" + company.getMaxUsers() + ").");
        }

        boolean alreadyPending = invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING).stream()
                .anyMatch(i -> i.getEmail().equalsIgnoreCase(email));
        if (alreadyPending) {
            throw new IllegalArgumentException("Já existe convite pendente para " + email);
        }
        if (inviteeAlreadyMember(email, companyId)) {
            throw new IllegalArgumentException("Este usuário já é membro desta empresa.");
        }

        String token = InvitationTokenService.generateToken();
        Invitation invitation = invitationRepository.save(
                Invitation.create(companyId, email, role, InvitationTokenService.hash(token), invitedBy));

        String tokenUrl = buildTokenUrl(token);
        emailSender.sendInvitation(email, company.getTradingName(), role, tokenUrl);

        auditor.record(companyId, AuditAction.CREATE, AuditModule.INVITATIONS, "Invitation",
                invitation.getId().toString(),
                "Convite criado para " + email,
                invitedBy, Map.of("email", email, "role", role));
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
        auditInvitationRevoked(invitation);
    }

    @Override
    @Transactional
    public InvitationResponse accept(String token, UUID userId) {
        if (!rateLimiter.tryAccept(userId.toString())) {
            throw new IllegalStateException("Muitas tentativas de aceite. Tente novamente mais tarde.");
        }
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

            // Enforcement max_users (Sprint 8.6): bloqueia aceite quando a empresa
            // já atingiu o limite de usuários do plano.
            Company target = companyRepository.findById(invitation.getCompanyId())
                    .orElseThrow(() -> new CompanyNotFoundException(invitation.getCompanyId()));
            if (membershipRepository.countActiveByCompanyId(invitation.getCompanyId()) >= target.getMaxUsers()) {
                throw new QuotaExceededException(
                        "Limite de usuários da empresa atingido (" + target.getMaxUsers() + ").");
            }

            membershipRepository.save(Membership.activate(user.getId(), invitation.getCompanyId(), invitation.getRole()));
            assignRole(user, invitation);
            user.grantCrmAccess();
            // Usuário sem empresa ativa (ex.: pós-onboarding pendente) passa a ter a
            // empresa convidada como ativa — completa o fluxo de aceite ponta a ponta e
            // a nova empresa já aparece ativa no Company Switcher (Sprint 8.4).
            // Quem já possui empresa ativa permanece nela (a convidada fica disponível
            // para troca manual).
            if (user.getCompanyId() == null) {
                user.setCompanyId(invitation.getCompanyId());
            }
            userRepository.save(user);

            invitation.accept();
            InvitationResponse response = toResponse(invitationRepository.save(invitation));

            // Auditoria de tenant (Sprint 8.6): aceite do convite + membership criada.
            auditInvitationAccepted(invitation, user, invitation.getRole());
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
            auditInvitationRevoked(invitation);
            TenantContext.clear();
            return response;
        } finally {
            tokenContext.clear();
        }
    }

    /** Auditoria: aceite de convite + criação de membership (Sprint 8.6). */
    private void auditInvitationAccepted(Invitation invitation, User user, String role) {
        UUID companyId = invitation.getCompanyId();
        UUID userId = user.getId();
        auditor.record(companyId, AuditAction.ASSIGN, AuditModule.INVITATIONS, "Invitation",
                invitation.getId().toString(),
                "Convite aceito por " + (user.getEmail() == null ? "membro" : user.getEmail().value()),
                userId, Map.of("email", String.valueOf(user.getEmail() == null ? "" : user.getEmail().value()),
                        "role", role));
        auditor.record(companyId, AuditAction.ASSIGN, AuditModule.MEMBERSHIPS, "Member",
                userId.toString(),
                "Membro adicionado à empresa via aceite de convite",
                userId, Map.of("role", role));
    }

    /** Auditoria: convite revogado/recusado (Sprint 8.6). */
    private void auditInvitationRevoked(Invitation invitation) {
        auditor.record(invitation.getCompanyId(), AuditAction.REJECT, AuditModule.INVITATIONS, "Invitation",
                invitation.getId().toString(),
                "Convite revogado/recusado para " + invitation.getEmail(),
                null, Map.of("email", invitation.getEmail()));
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

    /** True se já existe um membro ATIVO com o e-mail na empresa. */
    private boolean inviteeAlreadyMember(String email, UUID companyId) {
        return userRepository.findByEmail(email)
                .map(u -> membershipRepository.existsActiveByUserIdAndCompanyId(u.getId(), companyId))
                .orElse(false);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildTokenUrl(String token) {
        // Base absoluta opcional (configurada em produção p/ link clicável no
        // e-mail). Vazia por padrão → mantém caminho relativo (sem inventar domínio).
        if (invitationBaseUrl != null && !invitationBaseUrl.isBlank()) {
            return invitationBaseUrl.replaceAll("/+$", "") + "/invitations/accept?token=" + token;
        }
        return "/invitations/accept?token=" + token;
    }

    private static InvitationResponse toResponse(Invitation i) {
        return new InvitationResponse(i.getId(), i.getCompanyId(), i.getEmail(), i.getRole(),
                i.getStatus(), i.getInvitedBy(), i.getExpiresAt(), i.getCreatedAt());
    }
}