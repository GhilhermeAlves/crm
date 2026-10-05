package com.becommerce.crm.identity.application.service;

import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.domain.exception.IdentityServiceUnavailableException;
import com.becommerce.crm.identity.domain.exception.SignupOutcomeUnknownException;
import com.becommerce.crm.identity.domain.exception.UserProvisioningException;
import com.becommerce.crm.identity.infrastructure.client.AuthServiceClient;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Cadastro que envolve Keycloak e PostgreSQL, que não participam da mesma
 * transação.
 *
 * <ol>
 *   <li>Cria o usuário no Keycloak (fora de transação).</li>
 *   <li>Executa a etapa do PostgreSQL numa transação própria; o commit
 *       acontece DENTRO do try, então uma falha de commit também é tratada.</li>
 *   <li>Em qualquer falha, consulta numa transação NOVA se a linha
 *       {@code users.keycloak_sub} existe. Só apaga o usuário do Keycloak
 *       quando tem certeza de que o PostgreSQL não o gravou; se a linha existe
 *       (commit concluído apesar do erro), o cadastro é tratado como sucesso.
 *       Se nem a consulta funciona, mantém o Keycloak e responde 503.</li>
 * </ol>
 *
 * <p>Um usuário Keycloak órfão (exclusão falhou ou commit incerto) não acessa
 * o CRM sem a linha em {@code users}; uma nova tentativa recebe "e-mail já
 * cadastrado" e o primeiro login com a mesma senha o provisiona.
 */
@Component
public class KeycloakSignupSaga {

    private static final Logger log = LoggerFactory.getLogger(KeycloakSignupSaga.class);

    private final AuthServiceClient authServiceClient;
    private final UserRepository userRepository;
    private final TransactionTemplate persistTx;
    private final TransactionTemplate verifyTx;

    public KeycloakSignupSaga(AuthServiceClient authServiceClient,
                              UserRepository userRepository,
                              PlatformTransactionManager transactionManager) {
        this.authServiceClient = authServiceClient;
        this.userRepository = userRepository;
        this.persistTx = new TransactionTemplate(transactionManager);
        this.verifyTx = new TransactionTemplate(transactionManager);
        this.verifyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.verifyTx.setReadOnly(true);
    }

    /**
     * @param persistStep          etapa do PostgreSQL; recebe o id do usuário no Keycloak
     * @param resultIfPersisted    resultado quando o commit foi confirmado só pela verificação
     */
    public <T> T execute(String email, String rawPassword, String name,
                         Function<String, T> persistStep, Supplier<T> resultIfPersisted) {
        String keycloakUserId = createKeycloakUser(email, rawPassword, name);
        try {
            return persistTx.execute(status -> persistStep.apply(keycloakUserId));
        } catch (RuntimeException failure) {
            if (rowExists(keycloakUserId, email)) {
                log.warn("Cadastro concluído apesar do erro após o commit (keycloakUserId={}): {}",
                        keycloakUserId, failure.getMessage());
                return resultIfPersisted.get();
            }
            deleteKeycloakUser(keycloakUserId, email, failure);
            throw failure;
        }
    }

    private String createKeycloakUser(String email, String rawPassword, String name) {
        try {
            return authServiceClient.createKeycloakUser(email, rawPassword, name);
        } catch (DuplicateEmailException | IdentityServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new UserProvisioningException("Falha ao criar usuário no Keycloak: " + e.getMessage());
        }
    }

    /** true se a linha existe; lança {@link SignupOutcomeUnknownException} se não dá para saber. */
    private boolean rowExists(String keycloakUserId, String email) {
        String previousSub = TenantContext.getKeycloakSub();
        String previousEmail = TenantContext.getIdentityEmail();
        try {
            // Identidade do próprio usuário: permite ler a linha sob RLS (V025/V032).
            TenantContext.setKeycloakSub(keycloakUserId);
            TenantContext.setIdentityEmail(email);
            Boolean exists = verifyTx.execute(status ->
                    userRepository.findByKeycloakSub(keycloakUserId).isPresent());
            return Boolean.TRUE.equals(exists);
        } catch (RuntimeException verificationFailure) {
            log.error("KEYCLOAK_SIGNUP_UNCERTAIN keycloakUserId={} email={} cause={}",
                    keycloakUserId, email, verificationFailure.toString());
            throw new SignupOutcomeUnknownException();
        } finally {
            restore(previousSub, previousEmail);
        }
    }

    private void deleteKeycloakUser(String keycloakUserId, String email, RuntimeException cause) {
        try {
            authServiceClient.deleteKeycloakUser(keycloakUserId);
            log.warn("Cadastro desfeito: usuário Keycloak {} removido após falha no PostgreSQL: {}",
                    keycloakUserId, cause.getMessage());
        } catch (Exception deleteFailure) {
            log.error("KEYCLOAK_SIGNUP_ORPHAN keycloakUserId={} email={} cause={} deleteError={}",
                    keycloakUserId, email, cause.toString(), deleteFailure.toString());
        }
    }

    private static void restore(String previousSub, String previousEmail) {
        if (previousSub == null) {
            TenantContext.clearKeycloakSub();
        } else {
            TenantContext.setKeycloakSub(previousSub);
        }
        if (previousEmail == null) {
            TenantContext.clearIdentityEmail();
        } else {
            TenantContext.setIdentityEmail(previousEmail);
        }
    }
}
