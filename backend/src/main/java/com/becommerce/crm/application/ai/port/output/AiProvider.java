package com.becommerce.crm.application.ai.port.output;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provider de IA genérico de chat (AI-01/AI-03) — o "modelo" que produz a
 * resposta do assistente dado um conjunto de mensagens (sistema + contexto +
 * histórico + pergunta) e, opcionalmente, declaração de Tools (AI-03). O
 * domínio de CRM não conhece classes de provedor específico; os adapters
 * (OpenAI, etc.) implementam esta porta.
 *
 * <p>AI-03 (Tool Calling): o provider pode receber a definição das Tools e
 * responder com uma chamada de Tool ({@link ToolCall}) em vez de texto final.
 * A EXECUÇÃO da Tool é intermediada pelo backend (Tool Registry) — o provider
 * nunca executa a Tool; apenas solicita. {@link #chat} é mantido como atalho
 * de compatibilidade (AI-01/02) delegando a {@link #chatWithTools}.</p>
 */
public interface AiProvider {

    /**
     * Mensagem trocada com o modelo. Para o protocolo de Tool Calling:
     * {@code toolCalls} transporta as chamadas solicitadas pelo assistente
     * (mensagem devolvida ao modelo) e {@code toolCallId} vincula a resposta
     * de uma Tool (role "tool") à chamada correspondente.
     */
    record ChatMessage(String role, String content, List<ToolCall> toolCalls, String toolCallId) {

        public ChatMessage(String role, String content) {
            this(role, content, null, null);
        }
    }

    /** Definição de uma Tool enviada ao modelo (nome, descrição e schema de entrada). */
    record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {
    }

    /** Solicitação de execução de uma Tool feita pelo modelo ao backend. */
    record ToolCall(String id, String name, Map<String, Object> arguments) {
    }

    /** Resposta do modelo: texto final e/ou chamadas de Tool a executar. */
    record ChatResult(String content, List<ToolCall> toolCalls) {

        public static ChatResult content(String content) {
            return new ChatResult(content, List.of());
        }

        public static ChatResult withToolCalls(List<ToolCall> toolCalls) {
            return new ChatResult(null, toolCalls);
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    record ChatRequest(UUID companyId, UUID userId, List<ChatMessage> messages,
                       List<ToolDefinition> tools) {

        /** Construtor de compatibilidade (AI-01/02): sem Tools. */
        public ChatRequest(UUID companyId, UUID userId, List<ChatMessage> messages) {
            this(companyId, userId, messages, List.of());
        }
    }

    /**
     * Gera a resposta (possivelmente contendo chamadas de Tool) para as
     * mensagens e Tools informadas. Lança
     * {@link com.becommerce.crm.domain.ai.AiProviderException} em falha.
     */
    ChatResult chatWithTools(ChatRequest request);

    /**
     * Atalho de compatibilidade (AI-01/02): retorna apenas o texto final.
     * Default delega a {@link #chatWithTools}; providers podem sobrescrever.
     */
    default String chat(ChatRequest request) {
        return chatWithTools(request).content();
    }

    /**
     * Gera uma resposta ESTRUTURADA (AI-06): retorna o JSON bruto produzido pelo
     * modelo a partir das mensagens (sem tool calling), usado pela análise
     * contextual para obter resumo/inferências/recomendações dentro de um
     * contrato. O parsing é responsabilidade do chamador (backend), nunca regex
     * frágil. O default delega ao chat; providers devem usar JSON mode quando
     * suportado para máxima fidelidade.
     */
    default String chatStructured(ChatRequest request) {
        String content = chatWithTools(request).content();
        return content != null ? content.trim() : "{}";
    }

    /** Nome do provider (para logs/observabilidade, sem expor secrets). */
    String providerName();
}