package com.becommerce.crm.domain.ai;

/**
 * Acao de IA nao encontrada (ou de outra empresa/usuario). Nao expoe detalhes
 * que ajudem a enumerar IDs.
 */
public class AiActionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiActionNotFoundException(String message) {
        super(message);
    }
}