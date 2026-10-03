package com.becommerce.crm.automation.ai.infrastructure.persistence;

import com.becommerce.crm.automation.ai.application.port.output.AgentToolRepository;
import com.becommerce.crm.automation.ai.domain.AgentTool;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentToolRepositoryImpl implements AgentToolRepository {

    private final AgentToolJpaRepository jpaRepository;

    public AgentToolRepositoryImpl(AgentToolJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<AgentTool> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByToolNameAsc(companyId).stream()
                .map(AgentToolRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<AgentTool> findByCompanyAndName(UUID companyId, String toolName) {
        return jpaRepository.findByCompanyIdAndToolName(companyId, toolName)
                .map(AgentToolRepositoryImpl::toDomain);
    }

    @Override
    public Optional<AgentTool> findById(UUID id) {
        return jpaRepository.findById(id).map(AgentToolRepositoryImpl::toDomain);
    }

    @Override
    public AgentTool save(AgentTool tool) {
        return toDomain(jpaRepository.save(toEntity(tool)));
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }

    static AgentTool toDomain(AgentToolJpaEntity e) {
        return AgentTool.reconstitute(e.getId(), e.getCompanyId(), e.getToolName(),
                e.getDescription(), e.isEnabled(), e.getToolType(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static AgentToolJpaEntity toEntity(AgentTool tool) {
        AgentToolJpaEntity e = new AgentToolJpaEntity();
        e.setId(tool.getId());
        e.setCompanyId(tool.getCompanyId());
        e.setToolName(tool.getToolName());
        e.setDescription(tool.getDescription());
        e.setEnabled(tool.isEnabled());
        e.setToolType(tool.getToolType());
        e.setCreatedAt(tool.getCreatedAt());
        e.setUpdatedAt(tool.getUpdatedAt());
        return e;
    }
}
