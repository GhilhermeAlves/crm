package com.becommerce.crm.identity.membership.application.service;

import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.identity.membership.application.dto.MemberResponse;
import com.becommerce.crm.identity.membership.application.dto.MembershipResponse;
import com.becommerce.crm.identity.membership.application.port.output.MemberProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipProjection;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import com.becommerce.crm.identity.domain.Role;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.identity.domain.exception.RoleNotFoundException;
import com.becommerce.crm.identity.domain.valueobject.RoleName;
import com.becommerce.crm.identity.membership.domain.Membership;
import com.becommerce.crm.identity.membership.domain.MembershipStatus;
import com.becommerce.crm.identity.membership.domain.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private MembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private TenantAuditRecorder auditor;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(membershipRepository, roleRepository, userRoleRepository, auditor);
    }

    private Role role(RoleName name) {
        return Role.create(name.name(), COMPANY_ID);
    }

    private MemberProjection memberProjection(String role, String status) {
        return new MemberProjection() {
            @Override public UUID getUserId() { return USER_ID; }
            @Override public String getRole() { return role; }
            @Override public String getStatus() { return status; }
            @Override public java.time.LocalDateTime getJoinedAt() { return java.time.LocalDateTime.now(); }
            @Override public String getName() { return "Ghilherme Santos"; }
            @Override public String getEmail() { return "ghilherme007@gmail.com"; }
        };
    }

    // ------------------------------------------------------------------ list

    @Test
    void shouldListMembersOfOwnCompany() {
        when(membershipRepository.findMembersByCompanyIdAndStatus(COMPANY_ID, "ACTIVE"))
                .thenReturn(List.of(memberProjection("AGENT", "ACTIVE")));

        List<MemberResponse> members = service.listMembers(COMPANY_ID, COMPANY_ID, null);

        assertEquals(1, members.size());
        assertEquals(USER_ID, members.get(0).userId());
        assertEquals("AGENT", members.get(0).role());
        assertEquals("ACTIVE", members.get(0).status());
    }

    @Test
    void shouldListRemovedMembersWhenAsked() {
        when(membershipRepository.findMembersByCompanyIdAndStatus(COMPANY_ID, "REMOVED"))
                .thenReturn(List.of(memberProjection("AGENT", "REMOVED")));

        List<MemberResponse> members =
                service.listMembers(COMPANY_ID, COMPANY_ID, MembershipStatus.REMOVED);

        assertEquals(1, members.size());
        assertEquals("REMOVED", members.get(0).status());
        verify(membershipRepository, never()).findMembersByCompanyIdAndStatus(eq(COMPANY_ID), eq("ACTIVE"));
    }

    @Test
    void shouldDenyListMembersOfAnotherCompany() {
        assertThrows(CrmAccessDeniedException.class,
                () -> service.listMembers(UUID.randomUUID(), COMPANY_ID, null));
        verify(membershipRepository, never()).findMembersByCompanyIdAndStatus(any(), any());
    }

    @Test
    void shouldListMyMemberships() {
        MembershipProjection projection = new MembershipProjection() {
            @Override public UUID getCompanyId() { return COMPANY_ID; }
            @Override public String getCompanyName() { return "Empresa"; }
            @Override public String getRole() { return "ADMIN"; }
            @Override public String getStatus() { return "ACTIVE"; }
            @Override public java.time.LocalDateTime getJoinedAt() { return java.time.LocalDateTime.now(); }
        };
        when(membershipRepository.findMembershipsByUserId(USER_ID)).thenReturn(List.of(projection));

        List<MembershipResponse> memberships = service.listMyMemberships(USER_ID);

        assertEquals(1, memberships.size());
        assertEquals(COMPANY_ID, memberships.get(0).companyId());
        assertEquals("ADMIN", memberships.get(0).role());
    }

    // --------------------------------------------------------- updateMemberRole

    @Test
    void shouldUpdateMemberRoleAndSyncUserRoles() {
        Role agentRole = role(RoleName.AGENT);
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "ADMIN");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT.name(), COMPANY_ID))
                .thenReturn(Optional.of(agentRole));
        when(membershipRepository.countActiveAdminByCompanyId(COMPANY_ID)).thenReturn(2L);
        when(userRoleRepository.existsByUserIdAndRoleId(USER_ID, agentRole.getId())).thenReturn(false);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMemberRole(COMPANY_ID, USER_ID, "AGENT", COMPANY_ID, false);

        assertEquals("AGENT", membership.getRole());
        verify(userRoleRepository).deleteByUserIdAndCompanyId(USER_ID, COMPANY_ID);
        verify(userRoleRepository).save(any());
    }

    @Test
    void shouldDenyDemotingLastAdmin() {
        Role agentRole = role(RoleName.AGENT);
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "ADMIN");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT.name(), COMPANY_ID))
                .thenReturn(Optional.of(agentRole));
        when(membershipRepository.countActiveAdminByCompanyId(COMPANY_ID)).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "AGENT", COMPANY_ID, false));
        verify(userRoleRepository, never()).deleteByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldAllowPromotingLastAdminToSuperAdmin() {
        Role superAdminRole = role(RoleName.SUPER_ADMIN);
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "ADMIN");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(roleRepository.findByNameAndCompanyId(RoleName.SUPER_ADMIN.name(), COMPANY_ID))
                .thenReturn(Optional.of(superAdminRole));
        when(userRoleRepository.existsByUserIdAndRoleId(USER_ID, superAdminRole.getId())).thenReturn(false);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMemberRole(COMPANY_ID, USER_ID, "SUPER_ADMIN", COMPANY_ID, false);

        assertEquals("SUPER_ADMIN", membership.getRole());
        verify(userRoleRepository).deleteByUserIdAndCompanyId(USER_ID, COMPANY_ID);
        verify(userRoleRepository).save(any());
    }

    @Test
    void shouldRejectInvalidRole() {
        assertThrows(RoleNotFoundException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "NAO_EXISTE", COMPANY_ID, false));
    }

    @Test
    void shouldRejectRoleNotInCompany() {
        when(roleRepository.findByNameAndCompanyId(RoleName.MANAGER.name(), COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "MANAGER", COMPANY_ID, false));
    }

    @Test
    void shouldThrowWhenMemberNotFoundOnUpdate() {
        Role agentRole = role(RoleName.AGENT);
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT.name(), COMPANY_ID))
                .thenReturn(Optional.of(agentRole));
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThrows(MembershipNotFoundException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "AGENT", COMPANY_ID, false));
    }

    @Test
    void shouldDenyUpdateForAnotherCompany() {
        assertThrows(CrmAccessDeniedException.class,
                () -> service.updateMemberRole(UUID.randomUUID(), USER_ID, "AGENT", COMPANY_ID, false));
        verify(membershipRepository, never()).findActiveByUserIdAndCompanyId(any(), any());
    }

    // ------------------------------------------------------------ removeMember

    @Test
    void shouldRemoveMemberAndRevokeRoles() {
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "AGENT");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countActiveByCompanyId(COMPANY_ID)).thenReturn(2L);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

        service.removeMember(COMPANY_ID, USER_ID, COMPANY_ID, false);

        assertEquals("REMOVED", membership.getStatus().name());
        verify(userRoleRepository).deleteByUserIdAndCompanyId(USER_ID, COMPANY_ID);
    }

    @Test
    void shouldDenyRemovingLastActiveMember() {
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "AGENT");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countActiveByCompanyId(COMPANY_ID)).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> service.removeMember(COMPANY_ID, USER_ID, COMPANY_ID, false));
        verify(userRoleRepository, never()).deleteByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldDenyRemovingLastAdmin() {
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "ADMIN");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countActiveAdminByCompanyId(COMPANY_ID)).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> service.removeMember(COMPANY_ID, USER_ID, COMPANY_ID, false));
        verify(userRoleRepository, never()).deleteByUserIdAndCompanyId(any(), any());
    }
}
