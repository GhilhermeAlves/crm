package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.dto.RegisterRequest;
import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.domain.identity.exception.DuplicateEmailException;
import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    private static final UUID DEFAULT_COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String MOCK_KEYCLOAK_USER_ID = "aabbccdd-1234-5678-abcd-ef0123456789";

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "self", authService);
        ReflectionTestUtils.setField(authService, "provisioningEnabled", true);
        ReflectionTestUtils.setField(authService, "defaultRoleName", "AGENT");
        ReflectionTestUtils.setField(authService, "defaultCompanyId", DEFAULT_COMPANY_ID.toString());
    }

    @Test
    void shouldRegisterUserWithoutCompanySelfService() {
        when(userRepository.existsByEmail("registro@crm.local")).thenReturn(false);
        when(authServiceClient.createKeycloakUser(eq("registro@crm.local"), anyString(), eq("Registro Teste")))
                .thenReturn(MOCK_KEYCLOAK_USER_ID);
        when(passwordEncoder.encode("Kc!Valid1Aa1")).thenReturn("$2a$12$encodedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("registro@crm.local", "Kc!Valid1Aa1", "Registro Teste", null);
        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        // Self-service (Sprint 8.3): usuário criado SEM empresa, sem membership
        // e sem role tenant-specific — será direcionado ao onboarding.
        assertEquals(null, saved.getCompanyId());
        assertEquals("registro@crm.local", saved.getEmail().value());
        assertEquals(MOCK_KEYCLOAK_USER_ID, saved.getKeycloakSub());
        assertTrue(saved.isCrmEnabled());
        assertNotNull(saved.getPassword().value());
        assertTrue(saved.getPassword().value().startsWith("$2a$"));

        verify(authServiceClient).createKeycloakUser("registro@crm.local", "Kc!Valid1Aa1", "Registro Teste");

        // NÃO deve atribuir role nem criar membership automaticamente.
        verify(roleRepository, never()).findByNameAndCompanyId(anyString(), any());
        verify(membershipRepository, never()).save(any());

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(null, eventCaptor.getValue().companyId());
    }

    @Test
    void shouldRejectDuplicateEmailInCrm() {
        when(userRepository.existsByEmail("duplicado@crm.local")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("duplicado@crm.local", "Kc!Valid1Aa1", "Registro Teste", null);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(authServiceClient, never()).createKeycloakUser(anyString(), anyString(), anyString());
    }

    @Test
    void shouldCompensateKeycloakWhenCrmSaveFails() {
        when(userRepository.existsByEmail("fail@crm.local")).thenReturn(false);
        when(authServiceClient.createKeycloakUser(eq("fail@crm.local"), anyString(), anyString()))
                .thenReturn(MOCK_KEYCLOAK_USER_ID);
        when(passwordEncoder.encode("Kc!Valid1Aa1")).thenReturn("$2a$12$encodedpassword");
        when(userRepository.save(any(User.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("DB error"));

        RegisterRequest request = new RegisterRequest("fail@crm.local", "Kc!Valid1Aa1", "Registro Teste", null);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> authService.register(request));
        verify(authServiceClient).deleteKeycloakUser(MOCK_KEYCLOAK_USER_ID);
    }

    @Test
    void shouldRecoverFromDuplicateKeycloakUser() {
        when(userRepository.existsByEmail("race@crm.local")).thenReturn(false);
        when(authServiceClient.createKeycloakUser(eq("race@crm.local"), anyString(), anyString()))
                .thenReturn(MOCK_KEYCLOAK_USER_ID);
        when(passwordEncoder.encode("Kc!Valid1Aa1")).thenReturn("$2a$12$encodedpassword");
        when(userRepository.save(any(User.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("duplicate"));
        when(userRepository.findByKeycloakSub(MOCK_KEYCLOAK_USER_ID))
                .thenReturn(Optional.of(User.create(
                        new com.becommerce.crm.domain.identity.valueobject.Email("race@crm.local"),
                        com.becommerce.crm.domain.identity.valueobject.Password.fromHash("$2a$12$abcdefghijklmnopqrstuuPFGHIJKLMNOPQRSTUVWXYZab"),
                        "Registro", "Teste", DEFAULT_COMPANY_ID)));

        RegisterRequest request = new RegisterRequest("race@crm.local", "Kc!Valid1Aa1", "Registro Teste", null);
        authService.register(request);

        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }
}
