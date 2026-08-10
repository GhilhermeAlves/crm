package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceProvisioningTest {

    private static final String SUB = "78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";
    private static final String PREFERRED_USERNAME = "ghilherme007";

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks
    private AuthService authService;

    private CrmAccessService crmAccessService;

    private Company activeCompany;
    private Role agentRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "self", authService);
        ReflectionTestUtils.setField(authService, "provisioningEnabled", true);
        ReflectionTestUtils.setField(authService, "defaultRoleName", "AGENT");
        crmAccessService = new CrmAccessService(companyRepository);
        ReflectionTestUtils.setField(authService, "crmAccessService", crmAccessService);

        activeCompany = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, 500, null, null
        );

        agentRole = Role.createSystem(RoleName.AGENT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private User existingUser() {
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        existing.linkKeycloak(SUB);
        existing.grantCrmAccess();
        return existing;
    }

    private void allowActiveCompany() {
        when(companyRepository.findById(activeCompany.getId())).thenReturn(Optional.of(activeCompany));
    }

    @Test
    void shouldProvisionIdentityButDenyAccessUntilCrmAccessGranted() {
        ReflectionTestUtils.setField(authService, "defaultCompanyId", activeCompany.getId().toString());
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT, activeCompany.getId()))
                .thenReturn(Optional.of(agentRole));
        when(userRoleRepository.existsByUserIdAndRoleId(any(UUID.class), any(UUID.class))).thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Identidade é provisionada (usuário criado no CRM)...
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));

        // ...mas o acesso ao CRM NÃO é concedido automaticamente (crm_enabled = false).
        assertTrue(ex.getMessage().contains("crm_enabled"));
        verify(userRepository, times(1)).save(captor.capture());
        User provisioned = captor.getValue();
        assertEquals(SUB, provisioned.getKeycloakSub());
        assertEquals(activeCompany.getId(), provisioned.getCompanyId());
        assertFalse(provisioned.isCrmEnabled());
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
        verify(eventPublisher).publish(any(UserCreatedEvent.class));

        // O tenant foi definido no TenantContext ANTES do INSERT, garantindo que
        // o GUC app.current_company_id satisfaça o WITH CHECK do RLS (sem bypass).
        assertEquals(activeCompany.getId(), TenantContext.getCompanyId());
    }

    @Test
    void shouldReuseExistingUserWithCrmAccessOnSubsequentLogin() {
        User existing = existingUser();
        allowActiveCompany();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertEquals(existing, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDenyExistingUserWithoutCrmAccess() {
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        existing.linkKeycloak(SUB);

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        assertThrows(CrmAccessDeniedException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLinkAndSyncExistingUserFoundByEmail() {
        User existing = existingUser();
        existing.setKeycloakSub(null);
        allowActiveCompany();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertEquals(existing, result);
        assertEquals(SUB, result.getKeycloakSub());
        assertEquals("Ghilherme", result.getFirstName());
        assertEquals("Santos", result.getLastName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldReturnWinnerWhenConcurrentCreationRaces() {
        User winner = existingUser();
        allowActiveCompany();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty(), Optional.of(winner));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(authService, "defaultCompanyId", activeCompany.getId().toString());
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertEquals(winner, result);
        assertEquals(SUB, result.getKeycloakSub());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowWhenTokenHasNoSubject() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserProvisioningException.class,
                () -> authService.provisionKeycloakUser(null, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnExistingUserWhenProvisioningDisabled() {
        ReflectionTestUtils.setField(authService, "provisioningEnabled", false);
        User existing = existingUser();
        allowActiveCompany();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertEquals(existing, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDenyInactiveUserFoundBySub() {
        User existing = existingUser();
        existing.deactivate();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        assertThrows(CrmAccessDeniedException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));
    }

    @Test
    void shouldDenyInactiveUserWhenProvisioningDisabled() {
        ReflectionTestUtils.setField(authService, "provisioningEnabled", false);
        User existing = existingUser();
        existing.deactivate();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        assertThrows(CrmAccessDeniedException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));
    }

    @Test
    void shouldThrowWhenProvisioningDisabledAndUserUnknown() {
        ReflectionTestUtils.setField(authService, "provisioningEnabled", false);
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserProvisioningException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));
    }

    @Test
    void shouldProvisionUserWithoutCompanyWhenNoTenantConfigured() {
        // Sprint 8.3: sem AUTH_DEFAULT_COMPANY_ID, o usuário é provisionado SEM
        // empresa (onboarding pendente) em vez de lançar PROVISIONING_REQUIRED.
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertNotNull(result.getId());
        assertEquals(SUB, result.getKeycloakSub());
        assertNull(result.getCompanyId());
        assertFalse(result.isCrmEnabled());
        // Sem empresa: sem role nem membership (aguardando onboarding).
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(membershipRepository, never()).save(any(Membership.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldSignalProvisioningRequiredWhenConfiguredTenantInvalid() {
        ReflectionTestUtils.setField(authService, "defaultCompanyId", "not-a-uuid");
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserProvisioningException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));

        verify(userRepository, never()).save(any(User.class));
        assertNull(TenantContext.getCompanyId());
    }
}
