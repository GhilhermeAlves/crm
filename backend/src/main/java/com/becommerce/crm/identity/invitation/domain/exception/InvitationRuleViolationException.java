package com.becommerce.crm.identity.invitation.domain.exception;

/**
 * Regra de convite violada pelos dados enviados (papel inválido, e-mail já
 * convidado, convidado já membro, e-mail diferente do convite). Responde 400.
 */
public class InvitationRuleViolationException extends IllegalArgumentException {

    public InvitationRuleViolationException(String message) {
        super(message);
    }
}
