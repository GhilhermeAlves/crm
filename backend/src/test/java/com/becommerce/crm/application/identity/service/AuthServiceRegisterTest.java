package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.dto.RegisterRequest;
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
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    private static final UUID DEFAULT_COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Company defaultCompany;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "self", authService);
        ReflectionTestUtils.setField(authService, "provisioningEnabled", true);
        ReflectionTestUtils.setField(authService, "defaultRoleName", "AGENT");
        ReflectionTestUtils.setField(authService, "defaultCompanyId", DEFAULT_COMPANY_ID.toString());

        defaultCompany = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, null, null
        );
    }

    @Test
    void shouldRegisterUserWithDefaultCompanyWhenCompanyIdNotProvided() {
        when(userRepository.existsByEmail("registro@crm.local")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("registro@crm.local", "Kc!Valid1Aa1", "Registro Teste", null);
        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals(DEFAULT_COMPANY_ID, saved.getCompanyId());
        assertEquals("registro@crm.local", saved.getEmail().value());

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(DEFAULT_COMPANY_ID, eventCaptor.getValue().companyId());
    }

    @Test
    void shouldRegisterUserWithProvidedCompany() {
        when(userRepository.existsByEmail("registro2@crm.local")).thenReturn(false);
        when(companyRepository.findById(defaultCompany.getId())).thenReturn(Optional.of(defaultCompany));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("registro2@crm.local", "Kc!Valid1Aa1", "Registro Teste",
                defaultCompany.getId());
        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(defaultCompany.getId(), captor.getValue().getCompanyId());
    }

    @Test
    void shouldRejectRegistrationWhenProvidedCompanyDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.existsByEmail("registro3@crm.local")).thenReturn(false);
        when(companyRepository.findById(unknownId)).thenReturn(Optional.empty());

        RegisterRequest request = new RegisterRequest("registro3@crm.local", "Kc!Valid1Aa1", "Registro Teste", unknownId);

        assertThrows(IllegalStateException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectDuplicateEmailBeforePersisting() {
        when(userRepository.existsByEmail("duplicado@crm.local")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("duplicado@crm.local", "Kc!Valid1Aa1", "Registro Teste", null);

        assertThrows(IllegalStateException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
