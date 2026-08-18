package com.becommerce.crm.application.ai.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Provider de IA genérico de chat (AI-01) — o "modelo" que produz a resposta do
 * assistente dado um conjunto de mensagens (sistema + contexto + histórico +
 * pergunta). O domínio de CRM não conhece classes de provedor específico; os
 * adapters (OpenAI, fake, etc.) implementam esta porta.
 *
 * <p>É uma porta nova e separada de {@link AiSuggestionProvider} (sugestão de
 * resposta do Inbox) para não acoplar o chat ao caso específico de omnichannel.
 */
public interface AiProvider {

    /** Mensagem trocada com o modelo. */
    record ChatMessage(String role, String content) {
    }

    record ChatRequest(UUID companyId, UUID userId, List<ChatMessage> messages) {
    }

    /**
     * Gera a resposta do assistente para as mensagens informadas.
     * Lança {@link com.becommerce.crm.domain.ai.AiProviderException} em falha.
     */
    String chat(ChatRequest request);

    /** Nome do provider (para logs/observabilidade, sem expor secrets). */
    String providerName();
}
