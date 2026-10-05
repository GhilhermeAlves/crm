package com.becommerce.crm.identity.invitation.domain.exception;

import com.becommerce.crm.identity.invitation.domain.InvitationStatus;

/**
 * O convite existe mas não pode mais ser usado (já aceito, expirado ou
 * revogado). Mapeado para 410 Gone com o status no corpo, para a tela de
 * convite explicar o motivo.
 *
 * <p>Estende {@link IllegalStateException} para manter a semântica dos
 * chamadores que já tratavam esse caso como estado inválido.
 */
public class InvitationNoLongerValidException extends IllegalStateException {

    private final InvitationStatus status;

    public InvitationNoLongerValidException(InvitationStatus status) {
        super(messageFor(status));
        this.status = status;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    private static String messageFor(InvitationStatus status) {
        return switch (status) {
            case ACCEPTED -> "Este convite já foi aceito.";
            case EXPIRED -> "Este convite expirou. Peça um novo convite ao administrador da empresa.";
            case REVOKED -> "Este convite foi cancelado.";
            case PENDING -> "Convite pendente.";
        };
    }
}
