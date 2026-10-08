package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.application.port.output.AgentMemoryRepository;
import com.becommerce.crm.automation.ai.domain.AgentMemory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Fake em memória que imita o contrato do repositório (escopo por empresa,
 * ordenação por importância e recência). O RLS real é coberto pelo
 * {@code AgentMemoryIsolationIT}.
 */
class InMemoryAgentMemoryRepository implements AgentMemoryRepository {

    final Map<UUID, AgentMemory> store = new LinkedHashMap<>();

    @Override
    public AgentMemory save(AgentMemory memory) {
        store.put(memory.getId(), memory);
        return memory;
    }

    @Override
    public Optional<AgentMemory> findById(UUID companyId, UUID id) {
        return Optional.ofNullable(store.get(id)).filter(m -> m.getCompanyId().equals(companyId));
    }

    @Override
    public List<AgentMemory> findByContact(UUID companyId, UUID contactId, int limit) {
        return new ArrayList<>(store.values()).stream()
                .filter(m -> m.getCompanyId().equals(companyId) && m.getContactId().equals(contactId))
                .sorted(Comparator.comparingInt(AgentMemory::getImportance).reversed()
                        .thenComparing(AgentMemory::getUpdatedAt, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    @Override
    public void delete(UUID companyId, UUID id) {
        findById(companyId, id).ifPresent(m -> store.remove(m.getId()));
    }
}
