package com.becommerce.crm.domain.ai;

/**
 * Conversa de IA não encontrada (ou de outra empresa/usuário). Não expõe
 * detalhes que ajudem a enumerar IDs.
 */
public class AiConversationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiConversationNotFoundException(String message) {
        super(message);
    }
}
