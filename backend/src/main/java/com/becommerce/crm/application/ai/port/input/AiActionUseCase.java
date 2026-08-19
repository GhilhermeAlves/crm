package com.becommerce.crm.application.ai.port.input;

import com.becommerce.crm.application.ai.dto.AiActionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso de acoes de escrita do assistente de IA (AI-05). Confirmar
 * executa a proposta persistida; cancelar recusa uma proposta {@code PROPOSED};
 * listar retorna as acoes de uma conversa do usuario. {@code companyId},
 * {@code userId} e {@code permissions} vem do {@code CurrentUser} autenticado -
 * nunca do payload.
 */
public interface AiActionUseCase {

    /**
     * Confirma e executa uma acao proposta. Apenas o usuario que a propos pode
     * confirmar; somente o estado {@code PROPOSED} avanca. Estados terminais
     * sao idempotentes (nao reexecutam). Requer a permissao de negocio da
     * ferramenta.
     */
    AiActionResponse confirm(UUID companyId, UUID userId, List<String> permissions, UUID actionId);

    /**
     * Cancela uma proposta {@code PROPOSED}. Apenas o autor pode cancelar.
     */
    AiActionResponse cancel(UUID companyId, UUID userId, UUID actionId);

    /**
     * Lista as acoes de uma conversa do usuario, em ordem cronologica. Valida a
     * posse da conversa antes de expor qualquer acao.
     */
    List<AiActionResponse> listByConversation(UUID companyId, UUID userId, UUID conversationId);
}