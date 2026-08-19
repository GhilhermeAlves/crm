package com.becommerce.crm.domain.ai;

/**
 * Estado invalido para uma acao de IA (AI-05): confirmar/cancelar uma acao em
 * estado terminal, ou operar sobre uma acao que nao esta {@code PROPOSED}.
 * Carrega uma mensagem amigavel ao usuario.
 */
public class AiActionInvalidStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiActionInvalidStateException(String message) {
        super(message);
    }
}