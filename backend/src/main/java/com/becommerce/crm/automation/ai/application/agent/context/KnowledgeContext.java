package com.becommerce.crm.automation.ai.application.agent.context;

import java.util.List;

/**
 * Conhecimento consultável relevante (documentos, FAQs, base). Diferente de
 * memória (interações anteriores) e de contexto do CRM (dados operacionais).
 */
public record KnowledgeContext(List<Snippet> snippets) {

    public static final KnowledgeContext EMPTY = new KnowledgeContext(List.of());

    public KnowledgeContext {
        snippets = snippets == null ? List.of() : List.copyOf(snippets);
    }

    public record Snippet(String source, String content) {
    }
}
