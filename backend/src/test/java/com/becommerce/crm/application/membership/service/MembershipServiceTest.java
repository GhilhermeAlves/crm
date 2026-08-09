package com.becommerce.crm.application.membership.service;

import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.dto.MemberResponse;
import com.becommerce.crm.application.membership.dto.MembershipResponse;
import com.becommerce.crm.application.membership.port.output.MemberProjection;
import com.becommerce.crm.application.membership.port.output.MembershipProjection;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.identity.exception.RoleNotFoundException;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.domain.membership.exception.MembershipNotFoundException;
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

    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(membershipRepository, roleRepository, userRoleRepository);
    }

    private Role role(RoleName name) {
        return Role.create(name, COMPANY_ID);
    }

    private MemberProjection memberProjection(String role) {
        return new MemberProjection() {
            @Override public UUID getUserId() { return USER_ID; }
            @Override public String getRole() { return role; }
            @Override public java.time.LocalDateTime getJoinedAt() { return java.time.LocalDateTime.now(); }
            @Override public String getName() { return "Ghilherme Santos"; }
            @Override public String getEmail() { return "ghilherme007@gmail.com"; }
        };
    }

    // ------------------------------------------------------------------ list

    @Test
    void shouldListMembersOfOwnCompany() {
        when(membershipRepository.findActiveMembersByCompanyId(COMPANY_ID))
                .thenReturn(List.of(memberProjection("AGENT")));

        List<MemberResponse> members = service.listMembers(COMPANY_ID, COMPANY_ID);

        assertEquals(1, members.size());
        assertEquals(USER_ID, members.get(0).userId());
        assertEquals("AGENT", members.get(0).role());
        assertEquals("ACTIVE", members.get(0).status());
    }

    @Test
    void shouldDenyListMembersOfAnotherCompany() {
        assertThrows(CrmAccessDeniedException.class,
                () -> service.listMembers(UUID.randomUUID(), COMPANY_ID));
        verify(membershipRepository, never()).findActiveMembersByCompanyId(any());
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
        Role adminRole = role(RoleName.ADMIN);
        Role agentRole = role(RoleName.AGENT);
        Membership membership = Membership.activate(USER_ID, COMPANY_ID, "ADMIN");
        when(membershipRepository.findActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of(membership));
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT, COMPANY_ID))
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
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT, COMPANY_ID))
                .thenReturn(Optional.of(agentRole));
        when(membershipRepository.countActiveAdminByCompanyId(COMPANY_ID)).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "AGENT", COMPANY_ID, false));
        verify(userRoleRepository, never()).deleteByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldRejectInvalidRole() {
        assertThrows(RoleNotFoundException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "NAO_EXISTE", COMPANY_ID, false));
    }

    @Test
    void shouldRejectRoleNotInCompany() {
        when(roleRepository.findByNameAndCompanyId(RoleName.MANAGER, COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> service.updateMemberRole(COMPANY_ID, USER_ID, "MANAGER", COMPANY_ID, false));
    }

    @Test
    void shouldThrowWhenMemberNotFoundOnUpdate() {
        Role agentRole = role(RoleName.AGENT);
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT, COMPANY_ID))
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
