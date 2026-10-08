package com.becommerce.crm.automation.ai.application.port.output;

import com.becommerce.crm.automation.ai.domain.AgentMemory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência das memórias do agente (tabela {@code agent_memory}, V088).
 * Todas as consultas exigem {@code companyId} e {@code contactId} explícitos —
 * filtro de aplicação somado ao RLS FORCE (defesa em profundidade).
 */
public interface AgentMemoryRepository {

    AgentMemory save(AgentMemory memory);

    Optional<AgentMemory> findById(UUID companyId, UUID id);

    /** Memórias do contato, mais importantes e mais recentes primeiro. */
    List<AgentMemory> findByContact(UUID companyId, UUID contactId, int limit);

    void delete(UUID companyId, UUID id);
}
