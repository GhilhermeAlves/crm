package com.becommerce.crm.identity.membership.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.membership.application.dto.MemberResponse;
import com.becommerce.crm.identity.membership.application.dto.MembershipResponse;
import com.becommerce.crm.identity.membership.application.port.input.MembershipUseCase;
import com.becommerce.crm.identity.membership.application.port.output.MemberProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.identity.domain.Role;
import com.becommerce.crm.identity.domain.UserRole;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.identity.domain.exception.RoleNotFoundException;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.identity.membership.domain.MembershipStatus;
import com.becommerce.crm.identity.membership.domain.exception.MembershipNotFoundException;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestão de membresias (Sprint 8.2). Regras:
 * <ul>
 *   <li>Acesso restrito à PRÓPRIA empresa (sem contexto cross-tenant — o
 *       company switcher é a 8.4).</li>
 *   <li>Não é possível remover o último membro ativo da empresa.</li>
 *   <li>Não é possível rebaixar/remover o último ADMIN (nem auto-rebaixar-se
 *       quando for o último ADMIN).</li>
 *   <li>Rebaixamento troca {@code memberships.role} E sincroniza
 *       {@code user_roles} (revoga roles antigas, atribui a nova).</li>
 *   <li>Desligamento marca {@code REMOVED} e revoga TODAS as roles
 *       ({@code user_roles}) do usuário na empresa — membro desligado perde
 *       acesso (as policies RLS + o gate de membership na resolução reforçam).</li>
 * </ul>
 */
@Service
public class MembershipService implements MembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantAuditRecorder auditor;

    public MembershipService(MembershipRepository membershipRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             TenantAuditRecorder auditor) {
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID companyId, UUID requesterCompanyId,
                                            MembershipStatus status) {
        assertOwnCompany(companyId, requesterCompanyId);
        MembershipStatus filter = status == null ? MembershipStatus.ACTIVE : status;
        return membershipRepository.findMembersByCompanyIdAndStatus(companyId, filter.name()).stream()
                .map(this::mapToMemberResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> listMyMemberships(UUID userId) {
        // RLS membership_own_policy (keycloak_sub/e-mail) enxerga as memberships
        // do usuário em todas as empresas; a própria camada de autorização do
        // endpoint não precisa filtrar.
        return membershipRepository.findMembershipsByUserId(userId).stream()
                .map(this::mapToMembershipResponse)
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse updateMemberRole(UUID companyId, UUID userId, String role,
                                           UUID requesterCompanyId, boolean isCrmAdmin) {
        assertCompanyAccess(companyId, requesterCompanyId, isCrmAdmin);

        Role targetRole = resolveRoleForCompany(role, companyId);
        if (CurrentUser.CRM_ADMIN_ROLE.equals(targetRole.getName())) {
            throw new CrmAccessDeniedException("O papel CRM_ADMIN não pode ser atribuído pela API.");
        }
        assertNotPlatformMember(userId, companyId, isCrmAdmin);

        Membership membership = membershipRepository.findActiveByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membro não encontrado nesta empresa: " + userId));

        boolean demotingAdmin = membership.isAdminRole()
                && !Membership.isAdminLevelRole(targetRole.getName());
        if (demotingAdmin) {
            assertNotLastAdmin(companyId, membership);
        }

        membership.changeRole(targetRole.getName());
        membershipRepository.save(membership);
        syncUserRoles(userId, companyId, targetRole);

        log.info("Membro {} rebaixado/alterado para {} na empresa {}", userId, targetRole.getName(), companyId);
        return mapToMemberResponse(membership, userId);
    }

    @Override
    @Transactional
    public void removeMember(UUID companyId, UUID userId,
                             UUID requesterCompanyId, boolean isCrmAdmin) {
        assertCompanyAccess(companyId, requesterCompanyId, isCrmAdmin);
        assertNotPlatformMember(userId, companyId, isCrmAdmin);

        Membership membership = membershipRepository.findActiveByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membro não encontrado nesta empresa: " + userId));

        if (membership.isAdminRole()) {
            assertNotLastAdmin(companyId, membership);
        }
        assertNotLastMember(companyId);

        membership.remove();
        membershipRepository.save(membership);
        // Revoga o RBAC: sem roles, o membro desligado perde todas as permissões.
        userRoleRepository.deleteByUserIdAndCompanyId(userId, companyId);

        // Auditoria de tenant (Sprint 8.6): remoção/desativação de membership.
        auditor.record(companyId, AuditAction.DELETE, AuditModule.MEMBERSHIPS, "Member",
                userId.toString(),
                "Membro desligado da empresa",
                null, null);

        log.info("Membro {} desligado da empresa {}", userId, companyId);
    }

    private Role resolveRoleForCompany(String role, UUID companyId) {
        String roleName = role.trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
        return roleRepository.findByNameAndCompanyId(roleName, companyId)
                .orElseThrow(() -> new RoleNotFoundException(
                        "Role não existe nesta empresa: " + role));
    }

    private void syncUserRoles(UUID userId, UUID companyId, Role targetRole) {
        Optional<Role> platformRole = platformRoleHeldBy(userId, companyId);
        userRoleRepository.deleteByUserIdAndCompanyId(userId, companyId);
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, targetRole.getId())) {
            userRoleRepository.save(UserRole.assign(userId, targetRole.getId(), companyId));
        }
        // Trocar o papel da empresa não tira o CRM_ADMIN de quem o tem.
        platformRole.ifPresent(r -> userRoleRepository.save(UserRole.assign(userId, r.getId(), companyId)));
    }

    /** Só o CRM_ADMIN altera ou desliga um membro que tem o papel de plataforma. */
    private void assertNotPlatformMember(UUID userId, UUID companyId, boolean isCrmAdmin) {
        if (!isCrmAdmin && platformRoleHeldBy(userId, companyId).isPresent()) {
            throw new CrmAccessDeniedException("Este membro é administrador do CRM e não pode ser alterado.");
        }
    }

    private Optional<Role> platformRoleHeldBy(UUID userId, UUID companyId) {
        return roleRepository.findByNameAndCompanyId(CurrentUser.CRM_ADMIN_ROLE, companyId)
                .filter(r -> userRoleRepository.existsByUserIdAndRoleId(userId, r.getId()));
    }

    private void assertNotLastAdmin(UUID companyId, Membership membership) {
        long adminCount = membershipRepository.countActiveAdminByCompanyId(companyId);
        if (adminCount <= 1 && membership.isAdminRole()) {
            throw new IllegalStateException(
                    "Não é possível rebaixar/remover o último ADMIN da empresa.");
        }
    }

    private void assertNotLastMember(UUID companyId) {
        long activeCount = membershipRepository.countActiveByCompanyId(companyId);
        if (activeCount <= 1) {
            throw new IllegalStateException(
                    "Não é possível desligar o último membro ativo da empresa.");
        }
    }

    private void assertOwnCompany(UUID companyId, UUID requesterCompanyId) {
        if (!companyId.equals(requesterCompanyId)) {
            throw new CrmAccessDeniedException("Acesso a membros desta empresa não permitido.");
        }
    }

    private void assertCompanyAccess(UUID companyId, UUID requesterCompanyId, boolean isCrmAdmin) {
        if (!isCrmAdmin && !companyId.equals(requesterCompanyId)) {
            throw new CrmAccessDeniedException("Acesso a membros desta empresa não permitido.");
        }
    }

    private MemberResponse mapToMemberResponse(MemberProjection projection) {
        return new MemberResponse(
                projection.getUserId(),
                projection.getName(),
                projection.getEmail(),
                projection.getRole(),
                projection.getStatus(),
                projection.getJoinedAt(),
                projection.getSpecialty());
    }

    private MemberResponse mapToMemberResponse(Membership membership, UUID userId) {
        return new MemberResponse(
                userId,
                null,
                null,
                membership.getRole(),
                membership.getStatus().name(),
                membership.getJoinedAt(),
                membership.getSpecialty());
    }

    private MembershipResponse mapToMembershipResponse(MembershipProjection projection) {
        return new MembershipResponse(
                projection.getCompanyId(),
                projection.getCompanyName(),
                projection.getRole(),
                projection.getStatus(),
                projection.getJoinedAt());
    }
}
