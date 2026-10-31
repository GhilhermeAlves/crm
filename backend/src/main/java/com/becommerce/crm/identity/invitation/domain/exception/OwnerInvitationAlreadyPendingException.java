package com.becommerce.crm.identity.invitation.domain.exception;

/**
 * Já existe um convite de OWNER pendente para a empresa. O convite anterior
 * precisa ser revogado/regenerado antes de criar outro.
 */
public class OwnerInvitationAlreadyPendingException extends RuntimeException {

    public OwnerInvitationAlreadyPendingException(String message) {
        super(message);
    }
}
