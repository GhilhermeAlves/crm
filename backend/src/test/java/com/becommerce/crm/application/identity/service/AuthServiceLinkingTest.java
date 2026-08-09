package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.crm.domain.identity.exception.LinkingRequiredException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 7.2 — Account Linking. Garante que identidade de provedor externo
 * (ex.: Google) NUNCA é vinculada a uma conta local apenas pelo e-mail
 * (LinkingRequiredException) e que o vínculo explícito verifica a senha antes
 * de gravar o {@code keycloak_sub}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceLinkingTest {

    private static final String SUB = "google-78490eac-150e-44db-b2c4-d7999c1c3801";
    private static final String EMAIL = "ghilherme007@gmail.com";
    private static final String RAW_PASSWORD = "Kc!Valid1Aa1";

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks
    private AuthService authService;

    private CrmAccessService crmAccessService;
    private Company activeCompany;

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
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private User localAccount() {
        User local = User.create(new Email(EMAIL), new Password(RAW_PASSWORD),
                "Ghilherme", "Santos", activeCompany.getId());
        local.grantCrmAccess();
        return local;
    }

    private void stubCrmAccess() {
        when(companyRepository.findById(activeCompany.getId())).thenReturn(Optional.of(activeCompany));
    }

    @Test
    void shouldNeverAutoLinkExternalIdentityByEmail() {
        User local = localAccount();
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(local));

        assertThrows(LinkingRequiredException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, "ghilherme007",
                        "Ghilherme", "Santos", "google"));

        verify(userRepository, never()).save(any(User.class));
        assertNull(local.getKeycloakSub());
    }

    @Test
    void shouldNeverAutoLinkExternalIdentityByEmailWhenProvisioningDisabled() {
        ReflectionTestUtils.setField(authService, "provisioningEnabled", false);
        User local = localAccount();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(local));

        assertThrows(LinkingRequiredException.class,
                () -> authService.provisionKeycloakUser(SUB, EMAIL, "ghilherme007",
                        "Ghilherme", "Santos", "google"));

        verify(userRepository, never()).save(any(User.class));
        assertNull(local.getKeycloakSub());
    }

    @Test
    void shouldStillSyncLocalFallbackByEmailWithoutExternalProvider() {
        User local = localAccount();
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(local));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubCrmAccess();

        // provider = null → login local no realm; o fallback por e-mail segue
        // legítimo (Sprint 1) e NÃO é considerado auto-vinculação externa.
        User result = authService.provisionKeycloakUser(SUB, EMAIL, "ghilherme007",
                "Ghilherme", "Santos");

        assertEquals(SUB, result.getKeycloakSub());
        verify(userRepository, org.mockito.Mockito.times(1)).save(any(User.class));
    }

    @Test
    void shouldLinkLocalAccountAfterPasswordVerification() {
        User local = localAccount();
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(local));
        when(passwordEncoder.matches(RAW_PASSWORD, local.getPassword().value())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubCrmAccess();

        User result = authService.linkKeycloakIdentity(SUB, EMAIL, "Ghilherme", "Santos", RAW_PASSWORD);

        assertEquals(SUB, result.getKeycloakSub());
        // O contexto de identidade (GUC RLS) foi definido durante a operação e
        // limpo no finally — nunca vaza para a próxima requisição.
        assertNull(TenantContext.getKeycloakSub());
        assertNull(TenantContext.getIdentityEmail());
        verify(userRepository, org.mockito.Mockito.times(1)).save(any(User.class));
    }

    @Test
    void shouldRejectWrongPasswordOnLink() {
        User local = localAccount();
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(local));
        when(passwordEncoder.matches("errada", local.getPassword().value())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.linkKeycloakIdentity(SUB, EMAIL, "Ghilherme", "Santos", "errada"));

        verify(userRepository, never()).save(any(User.class));
        assertNull(local.getKeycloakSub());
    }

    @Test
    void shouldRejectBlankPasswordOnLinkWithoutEncoderCall() {
        User local = localAccount();
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(local));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.linkKeycloakIdentity(SUB, EMAIL, "Ghilherme", "Santos", "  "));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldBeIdempotentWhenSubAlreadyLinked() {
        User linked = localAccount();
        linked.linkKeycloak(SUB);

        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.of(linked));
        stubCrmAccess();

        // Já vinculado: retorna sem re-verificar a senha (replay/relogin).
        User result = authService.linkKeycloakIdentity(SUB, EMAIL, "Ghilherme", "Santos", RAW_PASSWORD);

        assertEquals(linked, result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFailWhenLocalAccountNotFound() {
        when(userRepository.findByKeycloakSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserProvisioningException.class,
                () -> authService.linkKeycloakIdentity(SUB, EMAIL, "Ghilherme", "Santos", RAW_PASSWORD));
    }

    @Test
    void shouldFailWhenSubMissingOnLink() {
        assertThrows(UserProvisioningException.class,
                () -> authService.linkKeycloakIdentity(null, EMAIL, "Ghilherme", "Santos", RAW_PASSWORD));
        assertNull(TenantContext.getKeycloakSub());
        assertNull(TenantContext.getIdentityEmail());
    }
}
