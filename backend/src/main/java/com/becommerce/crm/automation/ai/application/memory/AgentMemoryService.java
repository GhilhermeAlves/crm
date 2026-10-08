package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.application.port.output.AgentMemoryRepository;
import com.becommerce.crm.automation.ai.domain.AgentMemory;
import com.becommerce.crm.automation.ai.domain.MemorySource;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Capacidade de memória do agente: salvar, recuperar, atualizar e remover.
 * Não define {@code TenantContext} (o chamador — runtime ou admin — já o fez);
 * todas as operações recebem o escopo explícito e o revalidam.
 */
@Service
public class AgentMemoryService {

    /** Teto de memórias por contato consideradas na deduplicação. */
    private static final int DEDUP_SCAN = 50;

    private final AgentMemoryRepository repository;
    private final MemoryRetriever retriever;
    private final MemoryWritePolicy policy;

    public AgentMemoryService(AgentMemoryRepository repository, MemoryRetriever retriever, MemoryWritePolicy policy) {
        this.repository = repository;
        this.retriever = retriever;
        this.policy = policy;
    }

    /** Resultado de uma tentativa de gravação proposta pelo agente. */
    public record SaveOutcome(boolean saved, AgentMemory memory, String rejectionReason) {
        static SaveOutcome rejected(String reason) {
            return new SaveOutcome(false, null, reason);
        }
    }

    /**
     * Gravação proposta pelo agente: passa pela {@link MemoryWritePolicy} e
     * deduplica (conteúdo equivalente do mesmo contato apenas é renovado).
     */
    @Transactional
    public SaveOutcome save(UUID companyId, UUID agentConfigId, UUID contactId, MemoryType type, String content,
                            Integer importance, MemorySource source, UUID sourceMessageId) {
        if (contactId == null) {
            return SaveOutcome.rejected("contato não identificado; memória não registrada");
        }
        // A política de seleção vale para o que o AGENTE propõe; a equipe (USER)
        // registra manualmente e passa só pela validação do domínio.
        if (source != MemorySource.USER) {
            Optional<String> rejection = policy.rejectionReason(type, content);
            if (rejection.isPresent()) {
                return SaveOutcome.rejected(rejection.get());
            }
        } else if (type == null) {
            return SaveOutcome.rejected("tipo de memória inválido");
        }
        String key = MemoryWritePolicy.normalize(content);
        Optional<AgentMemory> duplicate = repository.findByContact(companyId, contactId, DEDUP_SCAN).stream()
                .filter(m -> m.belongsTo(companyId, contactId))
                .filter(m -> MemoryWritePolicy.normalize(m.getContent()).equals(key))
                .findFirst();
        if (duplicate.isPresent()) {
            AgentMemory existing = duplicate.get();
            existing.update(type, null, importance == null ? null
                    : Math.max(importance, existing.getImportance()), null);
            return new SaveOutcome(true, repository.save(existing), null);
        }
        AgentMemory created = AgentMemory.create(companyId, agentConfigId, contactId, type, content, importance,
                source, sourceMessageId, Map.of(), null);
        return new SaveOutcome(true, repository.save(created), null);
    }

    @Transactional(readOnly = true)
    public List<AgentMemory> retrieveRelevant(MemoryQuery query) {
        return retriever.retrieveRelevant(query);
    }

    @Transactional(readOnly = true)
    public List<AgentMemory> listByContact(UUID companyId, UUID contactId, int limit) {
        return repository.findByContact(companyId, contactId, limit).stream()
                .filter(m -> m.belongsTo(companyId, contactId))
                .toList();
    }

    @Transactional
    public Optional<AgentMemory> update(UUID companyId, UUID memoryId, MemoryType type, String content,
                                        Integer importance, LocalDateTime expiresAt) {
        return repository.findById(companyId, memoryId)
                .filter(m -> m.getCompanyId().equals(companyId))
                .map(m -> {
                    m.update(type, content, importance, expiresAt);
                    return repository.save(m);
                });
    }

    @Transactional
    public boolean delete(UUID companyId, UUID memoryId) {
        Optional<AgentMemory> memory = repository.findById(companyId, memoryId)
                .filter(m -> m.getCompanyId().equals(companyId));
        memory.ifPresent(m -> repository.delete(companyId, m.getId()));
        return memory.isPresent();
    }
}
