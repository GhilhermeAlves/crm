package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.application.service.AuthService;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.invitation.application.dto.CreateInvitationRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationLinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationPreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.application.port.input.InvitationUseCase;
import com.becommerce.crm.identity.invitation.application.port.output.InvitationRepository;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.communication.notification.application.EmailSender;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.masterdata.company.domain.CompanyStatus;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.identity.domain.Role;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.UserRole;
import com.becommerce.crm.identity.invitation.domain.Invitation;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNotFoundException;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.masterdata.quota.domain.exception.QuotaExceededException;
import com.becommerce.crm.identity.invitation.infrastructure.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Casos de uso de convites (Sprint 8.5).
 *
 * <p>Toda escrita ocorre em transação. O aceite/recusa primeiro resolve o
 * convite por token (policy RLS por token), com lock da linha, e só então troca
 * o tenant para a empresa-alvo para criar a membership. ADMIN/OWNER
 * administram convites; SUPER_ADMIN e OWNER não são roles concedíveis por
 * convite (OWNER é exclusivo do onboarding).
 *
 * <p>Cadastro por convite (quem ainda não tem conta): {@link #prepareSignup}
 * valida antes de criar o usuário no Keycloak e {@link #completeSignup} grava
 * usuário, membership, papel e aceite numa única transação. A orquestração com
 * o Keycloak e a compensação ficam em {@link InvitationSignupService}.
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
    private final AuthService authService;

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
                             AuthService authService,
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
        this.authService = authService;
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

        if (hasOtherPendingInvitation(companyId, email, null)) {
            throw new IllegalArgumentException("Já existe convite pendente para " + email);
        }
        if (inviteeAlreadyMember(email, companyId)) {
            throw new IllegalArgumentException("Este usuário já é membro desta empresa.");
        }

        String token = InvitationTokenService.generateToken();
        Invitation invitation = invitationRepository.save(
                Invitation.create(companyId, email, blankToNull(request.name()), role,
                        InvitationTokenService.hash(token), invitedBy));

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
        Invitation invitation = findInCompany(invitationId, companyId);
        invitation.revoke(); // declínio/revogação -> REVOKED
        invitationRepository.save(invitation);
        auditInvitationRevoked(invitation);
    }

    @Override
    @Transactional
    public InvitationLinkResponse regenerate(UUID companyId, UUID invitationId, boolean sendEmail,
                                             UUID requestedBy) {
        TenantContext.setCompanyId(companyId);
        Invitation invitation = findInCompany(invitationId, companyId);
        boolean wasExpired = invitation.effectiveStatus() == InvitationStatus.EXPIRED;

        String token = InvitationTokenService.generateToken();
        invitation.renew(InvitationTokenService.hash(token));
        // Um convite EXPIRED que volta a PENDING não pode colidir com outro
        // pendente para o mesmo e-mail (índice uq_invitations_pending_company_email).
        if (wasExpired && hasOtherPendingInvitation(companyId, invitation.getEmail(), invitation.getId())) {
            throw new IllegalArgumentException("Já existe outro convite pendente para " + invitation.getEmail());
        }
        Invitation saved = invitationRepository.save(invitation);

        String url = buildTokenUrl(token);
        if (sendEmail) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new CompanyNotFoundException(companyId));
            emailSender.sendInvitation(saved.getEmail(), company.getTradingName(), saved.getRole(), url);
        }
        auditor.record(companyId, AuditAction.UPDATE, AuditModule.INVITATIONS, "Invitation",
                saved.getId().toString(),
                (sendEmail ? "Convite reenviado para " : "Novo link de convite gerado para ") + saved.getEmail(),
                requestedBy, Map.of("email", saved.getEmail(), "sent", String.valueOf(sendEmail)));
        return new InvitationLinkResponse(toResponse(saved), url);
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationPreviewResponse preview(String token) {
        String hash = InvitationTokenService.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            Invitation invitation = invitationRepository.findByTokenHash(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            String companyName = companyRepository.findById(invitation.getCompanyId())
                    .map(Company::getTradingName)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            InvitationStatus status = invitation.effectiveStatus();
            boolean hasAccount = status == InvitationStatus.PENDING && accountExists(invitation.getEmail());
            return new InvitationPreviewResponse(companyName, invitation.getEmail(),
                    invitation.getInviteeName(), invitation.getRole(), status,
                    invitation.getExpiresAt(), hasAccount);
        } finally {
            tokenContext.clear();
        }
    }

    /**
     * Pré-validação do cadastro por convite, ANTES de criar o usuário no
     * Keycloak: convite pendente e válido, e-mail ainda sem conta.
     */
    @Transactional(readOnly = true)
    public InvitationResponse prepareSignup(String token) {
        String hash = InvitationTokenService.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            Invitation invitation = invitationRepository.findByTokenHash(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            assertUsable(invitation);
            if (accountExists(invitation.getEmail())) {
                throw accountAlreadyExists();
            }
            return toResponse(invitation);
        } finally {
            tokenContext.clear();
        }
    }

    /**
     * Grava o cadastro por convite: usuário do CRM, membership, papel e aceite.
     * Roda na transação aberta por {@link InvitationSignupService} (o commit e a
     * compensação do Keycloak ficam lá). O convite é relido com lock: se outra
     * requisição já o aceitou, falha sem criar nada.
     */
    @Transactional
    public InvitationResponse completeSignup(String token, String keycloakUserId,
                                             String encodedPassword, String name) {
        String hash = InvitationTokenService.hash(token);
        tokenContext.setTokenHash(hash);
        try {
            Invitation invitation = invitationRepository.findByTokenHashForUpdate(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            assertUsable(invitation);
            String email = invitation.getEmail();

            // Identidade do novo usuário durante toda a transação: o INSERT em
            // users (company_id NULL) passa pela identity_onboarding_insert_policy
            // (V032) e a checagem de e-mail enxerga a própria linha (V025).
            TenantContext.setKeycloakSub(keycloakUserId);
            TenantContext.setIdentityEmail(email);
            if (userRepository.existsByEmail(email)) {
                throw accountAlreadyExists();
            }
            User user = authService.provisionInvitedUser(keycloakUserId, email, encodedPassword, name);
            TenantContext.setKeycloakSub(keycloakUserId);
            TenantContext.setIdentityEmail(email);

            bindUserToCompany(invitation, user);
            invitation.accept();
            InvitationResponse response = toResponse(invitationRepository.save(invitation));
            auditInvitationAccepted(invitation, user, invitation.getRole());
            return response;
        } finally {
            tokenContext.clear();
            TenantContext.clear();
        }
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
            // Lock da linha: um segundo aceite simultâneo espera este terminar e
            // então encontra o convite ACCEPTED.
            Invitation invitation = invitationRepository.findByTokenHashForUpdate(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            assertUsable(invitation);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new com.becommerce.crm.identity.domain.exception.UserNotFoundException());
            if (!matchesEmail(invitation, user)) {
                throw new IllegalArgumentException(
                        "Este convite é destinado ao e-mail " + invitation.getEmail() + ".");
            }

            bindUserToCompany(invitation, user);
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
            Invitation invitation = invitationRepository.findByTokenHashForUpdate(hash)
                    .orElseThrow(() -> new InvitationNotFoundException("Convite inválido ou inexistente."));
            assertUsable(invitation);
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

    /**
     * Vincula o usuário à empresa do convite: membership ativa com o papel do
     * convite, papel RBAC e acesso ao CRM. Quem não tinha empresa ativa passa a
     * ter a empresa do convite; quem já tinha permanece nela (a nova fica
     * disponível no seletor de empresa).
     */
    private void bindUserToCompany(Invitation invitation, User user) {
        UUID companyId = invitation.getCompanyId();
        TenantContext.setCompanyId(companyId);
        if (membershipRepository.existsActiveByUserIdAndCompanyId(user.getId(), companyId)) {
            throw new IllegalStateException("Você já é membro desta empresa.");
        }

        // Enforcement max_users (Sprint 8.6): bloqueia aceite quando a empresa
        // já atingiu o limite de usuários do plano.
        Company target = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (membershipRepository.countActiveByCompanyId(companyId) >= target.getMaxUsers()) {
            throw new QuotaExceededException(
                    "Limite de usuários da empresa atingido (" + target.getMaxUsers() + ").");
        }

        membershipRepository.save(Membership.activate(user.getId(), companyId, invitation.getRole()));
        assignRole(user, invitation);
        user.grantCrmAccess();
        if (user.getCompanyId() == null) {
            user.setCompanyId(companyId);
        }
        userRepository.save(user);
    }

    /** Pendente e dentro da validade; caso contrário 410 com o status efetivo. */
    private static void assertUsable(Invitation invitation) {
        if (!invitation.isPending()) {
            throw new InvitationNoLongerValidException(invitation.getStatus());
        }
        if (invitation.isExpired()) {
            invitation.markExpired();
            throw new InvitationNoLongerValidException(InvitationStatus.EXPIRED);
        }
    }

    /** Checa conta existente sob a identidade do e-mail (RLS V025: só a própria linha). */
    private boolean accountExists(String email) {
        return withIdentityEmail(email, () -> userRepository.existsByEmail(email));
    }

    private static <T> T withIdentityEmail(String email, Supplier<T> action) {
        String previous = TenantContext.getIdentityEmail();
        TenantContext.setIdentityEmail(email);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                TenantContext.clearIdentityEmail();
            } else {
                TenantContext.setIdentityEmail(previous);
            }
        }
    }

    private static DuplicateEmailException accountAlreadyExists() {
        return new DuplicateEmailException(
                "Já existe uma conta com este e-mail. Entre com ela para aceitar o convite.");
    }

    private Invitation findInCompany(UUID invitationId, UUID companyId) {
        return invitationRepository.findById(invitationId)
                .filter(i -> i.getCompanyId().equals(companyId))
                .orElseThrow(() -> new InvitationNotFoundException("Convite não encontrado: " + invitationId));
    }

    private boolean hasOtherPendingInvitation(UUID companyId, String email, UUID excludeId) {
        return invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING).stream()
                .filter(i -> !i.getId().equals(excludeId))
                .anyMatch(i -> i.getEmail().equalsIgnoreCase(email));
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
                String.valueOf(userId),
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildTokenUrl(String token) {
        // Base absoluta opcional (configurada em produção p/ link clicável no
        // e-mail). Vazia por padrão → mantém caminho relativo (sem inventar domínio).
        if (invitationBaseUrl != null && !invitationBaseUrl.isBlank()) {
            return invitationBaseUrl.replaceAll("/+$", "") + "/convite/" + token;
        }
        return "/convite/" + token;
    }

    private static InvitationResponse toResponse(Invitation i) {
        return new InvitationResponse(i.getId(), i.getCompanyId(), i.getEmail(), i.getInviteeName(), i.getRole(),
                i.getStatus(), i.getInvitedBy(), i.getExpiresAt(), i.getCreatedAt());
    }
}
