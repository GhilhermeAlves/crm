package com.becommerce.crm.application.ai.port.input;

import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiChatResponse;
import com.becommerce.crm.application.ai.dto.AiConversationResponse;
import com.becommerce.crm.application.ai.dto.AiMessageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso do assistente de IA (AI-01/AI-02). {@code companyId}, {@code
 * userId} e {@code permissions} v�m do {@code CurrentUser} autenticado - nunca
 * do payload do cliente. As permiss�es alimentam o Context Engine para decidir
 * quais dados de contexto expor.
 */
public interface AiAssistantUseCase {

    AiChatResponse chat(UUID companyId, UUID userId, List<String> permissions, AiChatRequest request);

    /**
     * Lista as conversas do usu�rio na empresa ativa (AI-04), ordenadas por
     * atualiza��o mais recente. Sempre scoped por companyId + userId.
     */
    List<AiConversationResponse> listConversations(UUID companyId, UUID userId);

    /**
     * Retorna as mensagens de uma conversa do usu�rio (AI-04), em ordem
     * cronol�gica. Valida a posse (empresa + usu�rio) antes de expor qualquer
     * mensagem - conversa de outro usu�rio/empresa resulta em 404.
     */
    List<AiMessageResponse> getConversationMessages(UUID companyId, UUID userId, UUID conversationId);
}