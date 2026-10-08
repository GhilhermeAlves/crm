package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.application.port.output.AgentMemoryRepository;
import com.becommerce.crm.automation.ai.domain.AgentMemory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recuperação determinística: memórias do contato (empresa + contato), sem as
 * expiradas, ordenadas por importância e recência, limitadas a {@code limit}
 * para conter tokens. Filtra novamente o escopo em memória (defesa em
 * profundidade além do RLS).
 */
@Component
public class RankedMemoryRetriever implements MemoryRetriever {

    /** Busca alguns a mais para compensar expiradas descartadas. */
    private static final int OVERFETCH = 2;

    private final AgentMemoryRepository repository;

    public RankedMemoryRetriever(AgentMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AgentMemory> retrieveRelevant(MemoryQuery query) {
        LocalDateTime now = LocalDateTime.now();
        return repository.findByContact(query.companyId(), query.contactId(), query.limit() * OVERFETCH).stream()
                .filter(m -> m.belongsTo(query.companyId(), query.contactId()))
                .filter(m -> query.agentConfigId() == null || query.agentConfigId().equals(m.getAgentConfigId()))
                .filter(m -> !m.isExpired(now))
                .limit(query.limit())
                .toList();
    }
}
