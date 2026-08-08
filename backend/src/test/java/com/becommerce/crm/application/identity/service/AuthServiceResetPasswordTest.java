package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.identity.PasswordResetToken;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.event.PasswordChangedEvent;
import com.becommerce.crm.domain.identity.exception.InvalidTokenException;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Sprint 7.4 — Recuperação/REDEFINIÇÃO de senha.
 *
 * Garante que contas provenientes do Keycloak (com {@code keycloakSub})
 * delegam o reset REAL da credencial ao crm-auth-service (nunca gravam a nova
 * senha no hash local), enquanto contas legadas locais seguem o hash próprio.
 * Também garante que o contexto RLS ({@code app.current_reset_token}) é
 * definido durante o fluxo e limpo no finally.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceResetPasswordTest {

    private static final String TOKEN = "reset-token-abc-123";
    private static final String EMAIL = "ghilherme007@gmail.com";
    private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID COMPANY_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

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

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private User localAccount() {
        User user = User.create(new Email(EMAIL), new Password("Kc!Legado1X"), "Ghilherme", "Santos", COMPANY_ID);
        user.linkKeycloak("kz-sub-fixo");
        return user;
    }

    private User legacyAccount() {
        User user = User.create(new Email(EMAIL), new Password("Kc!Legado1X"), "Ghilherme", "Santos", COMPANY_ID);
        return user;
    }

    @Test
    void shouldResetPasswordInKeycloakForKeycloakAccount() {
        PasswordResetToken resetToken = PasswordResetToken.create(TOKEN, USER_ID, 60);
        User user = localAccount();
        when(passwordResetTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        authService.resetPassword(TOKEN, "Kc!Novo1Aa");

        verify(authServiceClient).resetPassword(user.getKeycloakSub(), EMAIL, "Kc!Novo1Aa");
        verify(userRepository, never()).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(eventPublisher).publish(any(PasswordChangedEvent.class));
        assertNull(TenantContext.getResetToken());
    }

    @Test
    void shouldFallbackToLocalHashForLegacyAccount() {
        PasswordResetToken resetToken = PasswordResetToken.create(TOKEN, USER_ID, 2);
        User user = legacyAccount();
        when(passwordResetTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.resetPassword(TOKEN, "Kc!Nova1Aa");

        verify(authServiceClient, never()).resetPassword(any(), any(), any());
        verify(passwordResetTokenRepository).save(resetToken);
        verify(eventPublisher).publish(any(PasswordChangedEvent.class));
        assertNull(TenantContext.getResetToken());
    }

    @Test
    void shouldRejectExpiredOrUsedToken() {
        PasswordResetToken used = PasswordResetToken.create(TOKEN, USER_ID, 2);
        used.markAsUsed();
        when(passwordResetTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(used));

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword(TOKEN, "Kc!Nova1Aa"));
        verifyNoInteractions(authServiceClient);
        verify(userRepository, never()).findById(USER_ID);
        assertNull(TenantContext.getResetToken());
    }

    @Test
    void shouldRejectUnknownToken() {
        when(passwordResetTokenRepository.findByToken("nao-existe")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword("nao-existe", "Kc!Nova1Aa"));
        assertNull(TenantContext.getResetToken());
    }

    @Test
    void shouldFailWhenUserNotFound() {
        PasswordResetToken valid = PasswordResetToken.create(TOKEN, USER_ID, 2);
        when(passwordResetTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(valid));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.resetPassword(TOKEN, "Kc!Nova1Aa"));
        assertNull(TenantContext.getResetToken());
    }

    @Test
    void shouldSetResetTokenContextDuringResetAndClearAfter() {
        PasswordResetToken valid = PasswordResetToken.create(TOKEN, USER_ID, 2);
        User user = legacyAccount();
        when(passwordResetTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(valid));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        authService.resetPassword(TOKEN, "Kc!Nova1Aa");

        assertNull(TenantContext.getResetToken());
        assertNull(TenantContext.getIdentityEmail());
    }
}