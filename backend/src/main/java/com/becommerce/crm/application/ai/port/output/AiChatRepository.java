package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.domain.ai.AiConversation;
import com.becommerce.crm.domain.ai.AiMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de conversas e mensagens do assistente de IA (AI-01). Todos os
 * acessos são scoped à empresa ativa (RLS) — o isolamento é garantido pela
 * infraestrutura de tenant.
 */
public interface AiChatRepository {

    AiConversation saveConversation(AiConversation conversation);

    Optional<AiConversation> findConversationById(UUID id);

    List<AiConversation> findConversationsByUser(UUID companyId, UUID userId);

    AiMessage saveMessage(AiMessage message);

    List<AiMessage> findMessagesByConversation(UUID conversationId);

    void deleteConversation(UUID id);
}
