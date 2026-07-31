package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.JwtProvider;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.RefreshTokenRepository;
import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Company activeCompany;
    private Role agentRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "self", authService);
        ReflectionTestUtils.setField(authService, "provisioningEnabled", true);
        ReflectionTestUtils.setField(authService, "defaultRoleName", "AGENT");

        activeCompany = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, null, null
        );

        agentRole = Role.createSystem(RoleName.AGENT);
    }

    @Test
    void shouldProvisionNewUserOnFirstLogin() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(companyRepository.findAll()).thenReturn(List.of(activeCompany));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByNameAndCompanyId(RoleName.AGENT, Role.SYSTEM_COMPANY_ID))
                .thenReturn(Optional.of(agentRole));
        when(userRoleRepository.existsByUserIdAndRoleId(any(UUID.class), any(UUID.class))).thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertNotNull(result.getId());
        assertEquals(SUB, result.getKeycloakSub());
        assertEquals(EMAIL, result.getEmail().value());
        assertEquals(activeCompany.getId(), result.getCompanyId());
        assertEquals("Ghilherme", result.getFirstName());
        assertEquals("Santos", result.getLastName());
        assertEquals("Ghilherme Santos", result.getName());

        verify(userRepository, times(1)).save(any(User.class));
        verify(userRoleRepository, times(1)).save(any(UserRole.class));
        verify(eventPublisher).publish(any(UserCreatedEvent.class));
    }

    @Test
    void shouldReuseExistingUserOnSubsequentLogin() {
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        existing.linkKeycloak(SUB);

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertEquals(existing, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLinkAndSyncExistingUserFoundByEmail() {
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());

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
        User winner = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        winner.linkKeycloak(SUB);

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty(), Optional.of(winner));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(companyRepository.findAll()).thenReturn(List.of(activeCompany));
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
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        existing.linkKeycloak(SUB);

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        User result = authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos");

        assertEquals(existing, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectInactiveUserFoundBySub() {
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        existing.linkKeycloak(SUB);
        existing.deactivate();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        assertThrows(UserProvisioningException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, PREFERRED_USERNAME, "Ghilherme", "Santos"));
    }

    @Test
    void shouldRejectInactiveUserWhenProvisioningDisabled() {
        ReflectionTestUtils.setField(authService, "provisioningEnabled", false);
        User existing = User.create(new Email(EMAIL), new Password("Kc!Valid1Aa1"),
                "Ghilherme", "Santos", activeCompany.getId());
        existing.linkKeycloak(SUB);
        existing.deactivate();

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(existing));

        assertThrows(UserProvisioningException.class,
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
}
