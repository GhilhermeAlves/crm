package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.domain.AgentMemory;

import java.util.List;

/**
 * Recupera as memórias relevantes para a interação atual. Ponto de extensão
 * para busca semântica/RAG: a implementação atual é determinística
 * ({@link RankedMemoryRetriever}).
 */
public interface MemoryRetriever {

    List<AgentMemory> retrieveRelevant(MemoryQuery query);
}
