package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.service.KeycloakSignupSaga;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.infrastructure.client.AuthServiceClient;
import com.becommerce.crm.identity.invitation.application.dto.InvitationRegisterRequest;
import com.becommerce.crm.identity.invitation.application.dto.InvitationResponse;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNotFoundException;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import com.becommerce.crm.identity.application.port.output.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cadastro por convite (usuário sem conta): pré-validação antes do Keycloak,
 * e-mail sempre vindo do convite e compensação quando a transação perde a
 * corrida para outra requisição.
 */
@ExtendWith(MockitoExtension.class)
class InvitationSignupServiceTest {

    private static final String TOKEN = "tok-abc";
    private static final String EMAIL = "convidado@empresa.com";
    private static final String PASSWORD = "Kc!Valid1Aa1";
    private static final String KC_ID = "kc-999";

    @Mock InvitationService invitationService;
    @Mock InvitationRateLimiter rateLimiter;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthServiceClient authServiceClient;
    @Mock UserRepository userRepository;
    @Mock PlatformTransactionManager txManager;

    InvitationSignupService signupService;

    private final UUID companyId = UUID.randomUUID();
    private InvitationResponse pendingInvitation;

    @BeforeEach
    void setUp() {
        KeycloakSignupSaga saga = new KeycloakSignupSaga(authServiceClient, userRepository, txManager);
        signupService = new InvitationSignupService(invitationService, saga, rateLimiter, passwordEncoder);
        lenient().when(rateLimiter.tryPublic(anyString())).thenReturn(true);
        lenient().when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$12$encoded");
        pendingInvitation = new InvitationResponse(UUID.randomUUID(), companyId, EMAIL, "Fulano", "AGENT",
                InvitationStatus.PENDING, UUID.randomUUID(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
    }

    private InvitationResponse accepted() {
        return new InvitationResponse(pendingInvitation.id(), companyId, EMAIL, "Fulano", "AGENT",
                InvitationStatus.ACCEPTED, pendingInvitation.invitedBy(), pendingInvitation.expiresAt(),
                pendingInvitation.createdAt());
    }

    private InvitationRegisterRequest request() {
        return new InvitationRegisterRequest(TOKEN, "Fulano de Tal", PASSWORD);
    }

    @Test
    void registersWithEmailFromInvitation() {
        when(invitationService.prepareSignup(TOKEN)).thenReturn(pendingInvitation);
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, "Fulano de Tal")).thenReturn(KC_ID);
        when(invitationService.completeSignup(TOKEN, KC_ID, "$2a$12$encoded", "Fulano de Tal"))
                .thenReturn(accepted());

        InvitationResponse response = signupService.register(request(), "10.0.0.1");

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        // o e-mail enviado ao Keycloak é o do convite (o request nem tem e-mail)
        verify(authServiceClient).createKeycloakUser(eq(EMAIL), eq(PASSWORD), eq("Fulano de Tal"));
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }

    @Test
    void invalidToken_failsBeforeKeycloak() {
        when(invitationService.prepareSignup(TOKEN)).thenThrow(new InvitationNotFoundException("x"));

        assertThrows(InvitationNotFoundException.class, () -> signupService.register(request(), "ip"));
        verify(authServiceClient, never()).createKeycloakUser(anyString(), anyString(), anyString());
    }

    @Test
    void alreadyAcceptedExpiredOrRevoked_failsBeforeKeycloak() {
        when(invitationService.prepareSignup(TOKEN)).thenThrow(
                new InvitationNoLongerValidException(InvitationStatus.ACCEPTED),
                new InvitationNoLongerValidException(InvitationStatus.EXPIRED),
                new InvitationNoLongerValidException(InvitationStatus.REVOKED));

        for (InvitationStatus expected : new InvitationStatus[]{
                InvitationStatus.ACCEPTED, InvitationStatus.EXPIRED, InvitationStatus.REVOKED}) {
            assertEquals(expected, assertThrows(InvitationNoLongerValidException.class,
                    () -> signupService.register(request(), "ip")).getStatus());
        }
        verify(authServiceClient, never()).createKeycloakUser(anyString(), anyString(), anyString());
    }

    @Test
    void existingUser_isSentToLoginWithoutTouchingKeycloak() {
        when(invitationService.prepareSignup(TOKEN)).thenThrow(new DuplicateEmailException("tem conta"));

        assertThrows(DuplicateEmailException.class, () -> signupService.register(request(), "ip"));
        verify(authServiceClient, never()).createKeycloakUser(anyString(), anyString(), anyString());
    }

    @Test
    void weakPassword_failsBeforeKeycloak() {
        when(invitationService.prepareSignup(TOKEN)).thenReturn(pendingInvitation);

        assertThrows(IllegalArgumentException.class, () -> signupService.register(
                new InvitationRegisterRequest(TOKEN, "Fulano", "123"), "ip"));
        verify(authServiceClient, never()).createKeycloakUser(anyString(), anyString(), anyString());
    }

    @Test
    void rateLimited_failsBeforeAnything() {
        when(rateLimiter.tryPublic("ip")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> signupService.register(request(), "ip"));
        verify(invitationService, never()).prepareSignup(anyString());
    }

    @Test
    void concurrentRequestWonTheInvitation_compensatesKeycloak() {
        // As duas requisições passam na pré-validação; esta perde o lock e
        // encontra o convite já ACCEPTED dentro da transação.
        when(invitationService.prepareSignup(TOKEN)).thenReturn(pendingInvitation);
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, "Fulano de Tal")).thenReturn(KC_ID);
        when(invitationService.completeSignup(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new InvitationNoLongerValidException(InvitationStatus.ACCEPTED));
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.empty());

        assertThrows(InvitationNoLongerValidException.class, () -> signupService.register(request(), "ip"));
        verify(authServiceClient).deleteKeycloakUser(KC_ID);
    }

    @Test
    void concurrentSameEmail_keycloakRejectsSecond_withoutCompensation() {
        // Mesmo e-mail em duas requisições (ex.: dois convites de empresas
        // diferentes): o Keycloak recusa o segundo usuário.
        when(invitationService.prepareSignup(TOKEN)).thenReturn(pendingInvitation);
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, "Fulano de Tal"))
                .thenThrow(new DuplicateEmailException("exists"));

        assertThrows(DuplicateEmailException.class, () -> signupService.register(request(), "ip"));
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }

    @Test
    void concurrentSameEmail_databaseRejectsSecond_compensatesKeycloak() {
        // Keycloak aceitou (corrida de criação), mas users.email UNIQUE barra o
        // segundo INSERT: o usuário Keycloak desta requisição é removido.
        when(invitationService.prepareSignup(TOKEN)).thenReturn(pendingInvitation);
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, "Fulano de Tal")).thenReturn(KC_ID);
        when(invitationService.completeSignup(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("users_email_key"));
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.empty());

        assertThrows(DataIntegrityViolationException.class, () -> signupService.register(request(), "ip"));
        verify(authServiceClient).deleteKeycloakUser(KC_ID);
    }

    @Test
    void commitOutcomeUnknownButPersisted_returnsAcceptedInvitation() {
        when(invitationService.prepareSignup(TOKEN)).thenReturn(pendingInvitation);
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, "Fulano de Tal")).thenReturn(KC_ID);
        when(invitationService.completeSignup(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(accepted());
        org.mockito.Mockito.doThrow(new org.springframework.transaction.TransactionSystemException("reset"))
                .doNothing().when(txManager).commit(any());
        when(userRepository.findByKeycloakSub(KC_ID))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(com.becommerce.crm.identity.domain.User.class)));

        InvitationResponse response = signupService.register(request(), "ip");

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        assertEquals(EMAIL, response.email());
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }
}
