package com.becommerce.crm.automation.ai.application.agent.context;

import java.util.List;

/**
 * Histórico recente da conversa: sequência temporal de mensagens (NÃO é
 * memória). Já vem podado pela janela/orçamento de tokens, em ordem cronológica,
 * sem a mensagem atual. {@code humanHandled} indica que a equipe atendeu a
 * conversa dentro dessa janela (o agente está retomando depois de um humano).
 */
public record ConversationHistory(List<Entry> entries, boolean humanHandled) {

    public static final ConversationHistory EMPTY = new ConversationHistory(List.of());

    public ConversationHistory {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public ConversationHistory(List<Entry> entries) {
        this(entries, false);
    }

    /** {@code role}: {@code user} (contato) ou {@code assistant} (agente/equipe). */
    public record Entry(String role, String content) {
    }
}
