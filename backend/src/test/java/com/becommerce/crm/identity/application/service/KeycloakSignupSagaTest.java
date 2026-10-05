package com.becommerce.crm.identity.application.service;

import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.domain.exception.IdentityServiceUnavailableException;
import com.becommerce.crm.identity.domain.exception.SignupOutcomeUnknownException;
import com.becommerce.crm.identity.infrastructure.client.AuthServiceClient;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cenários de falha entre Keycloak e PostgreSQL (fase 1 do convite).
 *
 * <p>Regra: o usuário do Keycloak só é apagado depois de confirmar, numa
 * transação nova, que o PostgreSQL NÃO possui a linha correspondente.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class KeycloakSignupSagaTest {

    private static final String EMAIL = "novo@empresa.com";
    private static final String PASSWORD = "Kc!Valid1Aa1";
    private static final String NAME = "Novo Usuario";
    private static final String KC_ID = "kc-123";

    @Mock AuthServiceClient authServiceClient;
    @Mock UserRepository userRepository;
    @Mock PlatformTransactionManager txManager;

    KeycloakSignupSaga saga;

    @BeforeEach
    void setUp() {
        saga = new KeycloakSignupSaga(authServiceClient, userRepository, txManager);
        org.mockito.Mockito.lenient().when(txManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void keycloakCreates() {
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, NAME)).thenReturn(KC_ID);
    }

    private String run(java.util.function.Function<String, String> step) {
        return saga.execute(EMAIL, PASSWORD, NAME, step, () -> "persisted-fallback");
    }

    @Test
    void success_commitsAndKeepsKeycloakUser() {
        keycloakCreates();

        String result = run(kcId -> "ok:" + kcId);

        assertEquals("ok:" + KC_ID, result);
        verify(txManager).commit(any());
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }

    @Test
    void scenario1_failureBeforeTransaction_deletesKeycloakUser() {
        keycloakCreates();
        CannotCreateTransactionException dbDown = new CannotCreateTransactionException("db down");
        when(txManager.getTransaction(any()))
                .thenThrow(dbDown)
                .thenReturn(new SimpleTransactionStatus());
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.empty());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> run(kcId -> "never"));

        assertSame(dbDown, thrown);
        verify(authServiceClient).deleteKeycloakUser(KC_ID);
    }

    @Test
    void scenario2_failureInsideTransaction_rollsBackAndDeletesKeycloakUser() {
        keycloakCreates();
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.empty());
        DataIntegrityViolationException boom = new DataIntegrityViolationException("membership");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> run(kcId -> {
            throw boom;
        }));

        assertSame(boom, thrown);
        verify(txManager).rollback(any());
        verify(authServiceClient).deleteKeycloakUser(KC_ID);
    }

    @Test
    void scenario3_commitFailsWithRollback_deletesKeycloakUser() {
        keycloakCreates();
        TransactionSystemException commitFailure = new TransactionSystemException("commit failed");
        doThrow(commitFailure).doNothing().when(txManager).commit(any());
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.empty());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> run(kcId -> "written"));

        assertSame(commitFailure, thrown);
        verify(authServiceClient).deleteKeycloakUser(KC_ID);
    }

    @Test
    void scenario4_commitOutcomeUnknownButRowExists_keepsKeycloakUserAndSucceeds() {
        keycloakCreates();
        doThrow(new TransactionSystemException("connection reset during commit"))
                .doNothing().when(txManager).commit(any());
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.of(mock(User.class)));

        String result = run(kcId -> "written");

        assertEquals("persisted-fallback", result);
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }

    @Test
    void scenario4_verificationAlsoFails_keepsKeycloakUserAndReportsUnknown(CapturedOutput output) {
        keycloakCreates();
        doThrow(new TransactionSystemException("connection reset during commit"))
                .doNothing().when(txManager).commit(any());
        when(userRepository.findByKeycloakSub(KC_ID))
                .thenThrow(new CannotCreateTransactionException("db still down"));

        assertThrows(SignupOutcomeUnknownException.class, () -> run(kcId -> "written"));

        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
        assertTrue(output.getOut().contains("KEYCLOAK_SIGNUP_UNCERTAIN"));
        assertTrue(output.getOut().contains(KC_ID));
    }

    @Test
    void scenario5_compensationDeleteFails_logsOrphanAndRethrowsOriginal(CapturedOutput output) {
        keycloakCreates();
        when(userRepository.findByKeycloakSub(KC_ID)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("keycloak down")).when(authServiceClient).deleteKeycloakUser(KC_ID);
        DataIntegrityViolationException original = new DataIntegrityViolationException("users_email_key");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> run(kcId -> {
            throw original;
        }));

        assertSame(original, thrown);
        assertTrue(output.getOut().contains("KEYCLOAK_SIGNUP_ORPHAN"));
        assertTrue(output.getOut().contains(KC_ID));
        assertTrue(output.getOut().contains(EMAIL));
    }

    @Test
    void verificationRunsWithIdentityContextOfTheNewUser() {
        keycloakCreates();
        AtomicReference<String> subSeen = new AtomicReference<>();
        when(userRepository.findByKeycloakSub(KC_ID)).thenAnswer(inv -> {
            subSeen.set(TenantContext.getKeycloakSub());
            return Optional.empty();
        });

        assertThrows(IllegalStateException.class, () -> run(kcId -> {
            throw new IllegalStateException("fail");
        }));

        assertEquals(KC_ID, subSeen.get());
        assertEquals(null, TenantContext.getKeycloakSub());
    }

    @Test
    void duplicateEmailInKeycloak_doesNotPersistNorCompensate() {
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, NAME))
                .thenThrow(new DuplicateEmailException("exists"));

        assertThrows(DuplicateEmailException.class, () -> run(kcId -> "never"));

        verify(txManager, never()).getTransaction(any());
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }

    @Test
    void keycloakUnavailable_doesNotPersistNorCompensate() {
        when(authServiceClient.createKeycloakUser(EMAIL, PASSWORD, NAME))
                .thenThrow(new IdentityServiceUnavailableException("down"));

        assertThrows(IdentityServiceUnavailableException.class, () -> run(kcId -> "never"));

        verify(txManager, never()).getTransaction(any());
        verify(authServiceClient, never()).deleteKeycloakUser(anyString());
    }

    @Test
    void successPathDoesNotQueryVerification() {
        keycloakCreates();
        doNothing().when(txManager).commit(any());

        run(kcId -> "ok");

        verify(userRepository, never()).findByKeycloakSub(anyString());
    }
}
