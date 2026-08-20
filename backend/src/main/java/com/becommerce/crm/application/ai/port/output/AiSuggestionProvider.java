package com.becommerce.crm.application.ai.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Abstração de provider de IA para sugestão de resposta (Sprint 20).
 * O domínio de CRM não conhece classes da OpenAI/LangChain; o adapter concreto
 * (OpenAI, etc.) implementa esta porta.
 */
public interface AiSuggestionProvider {

    /** Linha do histórico de conversa para o prompt. */
    record MessageLine(String role, String content) {
    }

    record SuggestRequest(UUID companyId, UUID conversationId, List<MessageLine> history) {
    }

    /**
     * Gera uma sugestão de resposta para a conversa, dado o histórico.
     * Lança {@link com.becommerce.crm.domain.ai.AiProviderException} em falha.
     */
    String suggest(SuggestRequest request);

    /** Nome do provider (para logs/observabilidade, sem expor secrets). */
    String providerName();
}