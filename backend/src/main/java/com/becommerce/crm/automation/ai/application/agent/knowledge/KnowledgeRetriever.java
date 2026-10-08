package com.becommerce.crm.automation.ai.application.agent.knowledge;

import com.becommerce.crm.automation.ai.application.agent.context.KnowledgeContext;

import java.util.UUID;

/**
 * Conhecimento consultável do agente (documentos, PDFs, FAQs, base). Ponto de
 * extensão para RAG: hoje não há fontes cadastradas, então a implementação
 * padrão ({@link NoKnowledgeRetriever}) devolve vazio. Sempre escopado por empresa.
 */
public interface KnowledgeRetriever {

    KnowledgeContext retrieve(UUID companyId, UUID agentConfigId, String currentMessage);
}
