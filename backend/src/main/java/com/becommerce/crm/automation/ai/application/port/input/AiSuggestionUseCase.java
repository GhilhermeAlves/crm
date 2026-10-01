package com.becommerce.crm.automation.ai.application.port.input;

import com.becommerce.crm.automation.ai.application.dto.AiSuggestionResponse;

import java.util.UUID;

/**
 * Caso de uso de sugestão de resposta com IA (Sprint 20).
 */
public interface AiSuggestionUseCase {

    /**
     * Gera uma sugestão de resposta para a conversa, considerando o histórico de
     * mensagens omnichannel. Requer que a conversa pertença à empresa ativa.
     */
    AiSuggestionResponse suggest(UUID companyId, UUID conversationId);
}