package com.becommerce.crm.application.membership.service;

import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.dto.MemberResponse;
import com.becommerce.crm.application.membership.dto.MembershipResponse;
import com.becommerce.crm.application.membership.port.input.MembershipUseCase;
import com.becommerce.crm.application.membership.port.output.MemberProjection;
import com.becommerce.crm.application.membership.port.output.MembershipProjection;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.identity.exception.RoleNotFoundException;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.domain.membership.exception.MembershipNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public MembershipService(MembershipRepository membershipRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository) {
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID companyId, UUID requesterCompanyId) {
        assertOwnCompany(companyId, requesterCompanyId);
        return membershipRepository.findActiveMembersByCompanyId(companyId).stream()
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
                                           UUID requesterCompanyId, boolean isSuperAdmin) {
        assertCompanyAccess(companyId, requesterCompanyId, isSuperAdmin);

        Role targetRole = resolveRoleForCompany(role, companyId);

        Membership membership = membershipRepository.findActiveByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membro não encontrado nesta empresa: " + userId));

        boolean demotingAdmin = membership.isAdminRole()
                && !targetRole.getName().equals(membership.getRole());
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
                             UUID requesterCompanyId, boolean isSuperAdmin) {
        assertCompanyAccess(companyId, requesterCompanyId, isSuperAdmin);

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

        log.info("Membro {} desligado da empresa {}", userId, companyId);
    }

    private Role resolveRoleForCompany(String role, UUID companyId) {
        String roleName = role.trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
        return roleRepository.findByNameAndCompanyId(roleName, companyId)
                .orElseThrow(() -> new RoleNotFoundException(
                        "Role não existe nesta empresa: " + role));
    }

    private void syncUserRoles(UUID userId, UUID companyId, Role targetRole) {
        userRoleRepository.deleteByUserIdAndCompanyId(userId, companyId);
        if (!userRoleRepository.existsByUserIdAndRoleId(userId, targetRole.getId())) {
            userRoleRepository.save(UserRole.assign(userId, targetRole.getId(), companyId));
        }
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

    private void assertCompanyAccess(UUID companyId, UUID requesterCompanyId, boolean isSuperAdmin) {
        if (!isSuperAdmin && !companyId.equals(requesterCompanyId)) {
            throw new CrmAccessDeniedException("Acesso a membros desta empresa não permitido.");
        }
    }

    private MemberResponse mapToMemberResponse(MemberProjection projection) {
        return new MemberResponse(
                projection.getUserId(),
                projection.getName(),
                projection.getEmail(),
                projection.getRole(),
                "ACTIVE",
                projection.getJoinedAt());
    }

    private MemberResponse mapToMemberResponse(Membership membership, UUID userId) {
        return new MemberResponse(
                userId,
                null,
                null,
                membership.getRole(),
                membership.getStatus().name(),
                membership.getJoinedAt());
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
