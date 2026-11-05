package com.becommerce.crm.automation.ai.application.port.input;

import com.becommerce.crm.automation.ai.application.dto.AiChatRequest;
import com.becommerce.crm.automation.ai.application.dto.AiChatResponse;
import com.becommerce.crm.automation.ai.application.dto.AiConversationResponse;
import com.becommerce.crm.automation.ai.application.dto.AiMessageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso do assistente de IA (AI-01/AI-02). {@code companyId}, {@code
 * userId} e {@code permissions} vêm do {@code CurrentUser} autenticado - nunca
 * do payload do cliente. As permissões alimentam o Context Engine para decidir
 * quais dados de contexto expor.
 */
public interface AiAssistantUseCase {

    AiChatResponse chat(UUID companyId, UUID userId, List<String> permissions, AiChatRequest request);

    /**
     * Lista as conversas do usuário na empresa ativa (AI-04), ordenadas por
     * atualização mais recente. Sempre scoped por companyId + userId.
     */
    List<AiConversationResponse> listConversations(UUID companyId, UUID userId);

    /**
     * Retorna as mensagens de uma conversa do usuário (AI-04), em ordem
     * cronológica. Valida a posse (empresa + usuário) antes de expor qualquer
     * mensagem - conversa de outro usuário/empresa resulta em 404.
     */
    List<AiMessageResponse> getConversationMessages(UUID companyId, UUID userId, UUID conversationId);
}