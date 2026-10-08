package com.becommerce.crm.automation.ai.infrastructure.persistence;

import com.becommerce.crm.automation.ai.application.port.output.AgentMemoryRepository;
import com.becommerce.crm.automation.ai.domain.AgentMemory;
import com.becommerce.crm.automation.ai.domain.MemorySource;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentMemoryRepositoryImpl implements AgentMemoryRepository {

    private final AgentMemoryJpaRepository jpaRepository;

    public AgentMemoryRepositoryImpl(AgentMemoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AgentMemory save(AgentMemory memory) {
        return toDomain(jpaRepository.save(toEntity(memory)));
    }

    @Override
    public Optional<AgentMemory> findById(UUID companyId, UUID id) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(AgentMemoryRepositoryImpl::toDomain);
    }

    @Override
    public List<AgentMemory> findByContact(UUID companyId, UUID contactId, int limit) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByImportanceDescUpdatedAtDesc(
                        companyId, contactId, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(AgentMemoryRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(UUID companyId, UUID id) {
        jpaRepository.deleteByIdAndCompanyId(id, companyId);
    }

    private static AgentMemoryJpaEntity toEntity(AgentMemory m) {
        AgentMemoryJpaEntity e = new AgentMemoryJpaEntity();
        e.setId(m.getId());
        e.setCompanyId(m.getCompanyId());
        e.setAgentConfigId(m.getAgentConfigId());
        e.setContactId(m.getContactId());
        e.setMemoryType(m.getType().name());
        e.setContent(m.getContent());
        e.setImportance((short) m.getImportance());
        e.setSource(m.getSource().name());
        e.setSourceMessageId(m.getSourceMessageId());
        e.setMetadata(new HashMap<>(m.getMetadata()));
        e.setExpiresAt(m.getExpiresAt());
        e.setCreatedAt(m.getCreatedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }

    private static AgentMemory toDomain(AgentMemoryJpaEntity e) {
        return AgentMemory.reconstitute(e.getId(), e.getCompanyId(), e.getAgentConfigId(), e.getContactId(),
                MemoryType.valueOf(e.getMemoryType()), e.getContent(), e.getImportance(),
                MemorySource.valueOf(e.getSource()), e.getSourceMessageId(), e.getMetadata(),
                e.getExpiresAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
