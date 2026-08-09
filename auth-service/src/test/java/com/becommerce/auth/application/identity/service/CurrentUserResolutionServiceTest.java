package com.becommerce.auth.application.identity.service;

import com.becommerce.auth.application.company.port.output.CompanyRepository;
import com.becommerce.auth.application.identity.port.output.MembershipRepository;
import com.becommerce.auth.application.identity.port.output.PermissionRepository;
import com.becommerce.auth.application.identity.port.output.RoleRepository;
import com.becommerce.auth.application.identity.port.output.UserRepository;
import com.becommerce.auth.domain.company.CompanyStatus;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.User;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserResolutionServiceTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CompanyRepository companyRepository;

    private CurrentUserResolutionService service;

    @BeforeEach
    void setUp() {
        service = new CurrentUserResolutionService(userRepository, roleRepository, permissionRepository, membershipRepository, companyRepository);
    }

    private AuthenticatedIdentity identity() {
        return new AuthenticatedIdentity(SUB, EMAIL, EMAIL, "Ghilherme", "Santos", "Ghilherme Santos", "session-1", "keycloak");
    }

    private User user(boolean active, boolean crmEnabled) {
        return new User(USER_ID, EMAIL, "Ghilherme", "Santos", "Ghilherme Santos", SUB, COMPANY_ID, active, crmEnabled);
    }

    private User activeUser() {
        return user(true, true);
    }

    private void allowActiveCompany() {
        when(companyRepository.findStatusById(COMPANY_ID)).thenReturn(Optional.of(CompanyStatus.ACTIVE));
    }

    @Test
    void shouldResolveCurrentUserForExistingAuthenticatedUser() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        allowActiveCompany();
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(true);
        when(membershipRepository.findMembershipRoleByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(Optional.of("AGENT"));
        when(roleRepository.findRoleNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(List.of("AGENT"));
        when(permissionRepository.findPermissionNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(List.of("contact:read", "dashboard:view"));

        CurrentUserResolution resolution = service.resolve(identity());

        assertInstanceOf(CurrentUserResolution.Resolved.class, resolution);
        CurrentUser currentUser = ((CurrentUserResolution.Resolved) resolution).currentUser();
        assertEquals(USER_ID, currentUser.userId());
        assertEquals(EMAIL, currentUser.email());
        assertEquals(COMPANY_ID, currentUser.companyId());
        assertEquals(SUB, currentUser.keycloakSub());
        assertEquals("session-1", currentUser.sessionId());
        assertEquals("keycloak", currentUser.provider());
        assertEquals(List.of("AGENT"), currentUser.roles());
        assertEquals(List.of("contact:read", "dashboard:view"), currentUser.permissions());
        assertEquals("AGENT", currentUser.membershipRole());

        verify(userRepository).findByKeycloakSub(SUB);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void shouldSignalProvisioningRequiredForUnknownUser() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        CurrentUserResolution resolution = service.resolve(identity());

        assertInstanceOf(CurrentUserResolution.ProvisioningRequired.class, resolution);
        AuthenticatedIdentity echoed = ((CurrentUserResolution.ProvisioningRequired) resolution).identity();
        assertEquals(SUB, echoed.keycloakSub());
        assertEquals(EMAIL, echoed.email());
        verify(roleRepository, never()).findRoleNamesByUserIdAndCompanyId(any(), any());
    }

    // ------------------------------------------------------------- gates (Sprint 6)

    @Test
    void shouldDenyInactiveUser() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user(false, true)));

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class, () -> service.resolve(identity()));

        assertTrue(ex.getMessage().contains("inativo"));
        verify(roleRepository, never()).findRoleNamesByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldDenyUserWithoutCrmAccess() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(user(true, false)));

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class, () -> service.resolve(identity()));

        assertTrue(ex.getMessage().contains("crm_enabled"));
        verify(roleRepository, never()).findRoleNamesByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldDenyUserWhenCompanySuspended() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        when(companyRepository.findStatusById(COMPANY_ID)).thenReturn(Optional.of(CompanyStatus.SUSPENDED));

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class, () -> service.resolve(identity()));

        assertTrue(ex.getMessage().contains("SUSPENDED"));
        verify(roleRepository, never()).findRoleNamesByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldDenyUserWhenCompanyInactive() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        when(companyRepository.findStatusById(COMPANY_ID)).thenReturn(Optional.of(CompanyStatus.INACTIVE));

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class, () -> service.resolve(identity()));

        assertTrue(ex.getMessage().contains("INACTIVE"));
        verify(roleRepository, never()).findRoleNamesByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldDenyUserWhenCompanyDoesNotExist() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        when(companyRepository.findStatusById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(CrmAccessDeniedException.class, () -> service.resolve(identity()));
    }

    @Test
    void shouldDenyUserWithoutActiveMembership() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        allowActiveCompany();
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(false);

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class, () -> service.resolve(identity()));

        assertTrue(ex.getMessage().contains("membership ativa"));
        verify(roleRepository, never()).findRoleNamesByUserIdAndCompanyId(any(), any());
    }

    @Test
    void shouldResolveRolesAndPermissionsFromCrm() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        allowActiveCompany();
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(true);
        when(roleRepository.findRoleNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(List.of("AGENT", "MANAGER"));
        when(permissionRepository.findPermissionNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID))
                .thenReturn(List.of("lead:read", "contact:read", "dashboard:view"));

        CurrentUserResolution resolution = service.resolve(identity());

        CurrentUser currentUser = ((CurrentUserResolution.Resolved) resolution).currentUser();
        assertEquals(List.of("AGENT", "MANAGER"), currentUser.roles());
        assertEquals(List.of("lead:read", "contact:read", "dashboard:view"), currentUser.permissions());
        verify(roleRepository).findRoleNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID);
        verify(permissionRepository).findPermissionNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID);
    }

    @Test
    void shouldResolveCompanyAndDeriveTenantFromCompany() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(activeUser()));
        allowActiveCompany();
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(true);
        when(roleRepository.findRoleNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(List.of());
        when(permissionRepository.findPermissionNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(List.of());

        CurrentUserResolution resolution = service.resolve(identity());

        CurrentUser currentUser = ((CurrentUserResolution.Resolved) resolution).currentUser();
        assertEquals(COMPANY_ID, currentUser.companyId());
        assertEquals(COMPANY_ID, currentUser.tenantId());
    }

    @Test
    void shouldResolveUserByEmailWhenSubIsUnknown() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));
        allowActiveCompany();
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(true);
        when(roleRepository.findRoleNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(List.of("AGENT"));
        when(permissionRepository.findPermissionNamesByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(List.of());

        CurrentUserResolution resolution = service.resolve(identity());

        assertInstanceOf(CurrentUserResolution.Resolved.class, resolution);
        assertEquals(USER_ID, ((CurrentUserResolution.Resolved) resolution).currentUser().userId());
        verify(userRepository).findByEmail(EMAIL);
    }
}
