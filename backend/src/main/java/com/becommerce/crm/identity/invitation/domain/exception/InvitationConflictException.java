package com.becommerce.crm.identity.invitation.domain.exception;

/**
 * Estado atual impede a operação de convite (empresa inativa, limite de
 * tentativas, usuário já membro). Responde 409.
 */
public class InvitationConflictException extends IllegalStateException {

    public InvitationConflictException(String message) {
        super(message);
    }
}
