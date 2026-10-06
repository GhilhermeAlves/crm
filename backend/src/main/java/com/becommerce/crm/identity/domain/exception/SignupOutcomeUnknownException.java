package com.becommerce.crm.identity.domain.exception;

/**
 * O usuário foi criado no Keycloak, a gravação no PostgreSQL falhou de forma
 * ambígua e nem a verificação posterior conseguiu confirmar se o commit
 * aconteceu. O usuário do Keycloak é mantido; a orientação é tentar entrar.
 */
public class SignupOutcomeUnknownException extends IdentityServiceUnavailableException {

    public SignupOutcomeUnknownException() {
        super("Não foi possível confirmar o seu cadastro agora. Tente entrar com o e-mail e a senha "
                + "informados; se não conseguir, tente se cadastrar novamente em alguns minutos.");
    }
}
