package com.becommerce.crm.infrastructure.ai.persistence;

import com.becommerce.crm.application.ai.port.output.AgentConfigRepository;
import com.becommerce.crm.domain.ai.AgentConfig;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentConfigRepositoryImpl implements AgentConfigRepository {

    private final AgentConfigJpaRepository jpaRepository;

    public AgentConfigRepositoryImpl(AgentConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AgentConfig> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).map(AgentConfigRepositoryImpl::toDomain);
    }

    @Override
    public AgentConfig save(AgentConfig agentConfig) {
        return toDomain(jpaRepository.save(toEntity(agentConfig)));
    }

    private static AgentConfigJpaEntity toEntity(AgentConfig c) {
        AgentConfigJpaEntity e = new AgentConfigJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setAiEnabled(c.isAiEnabled());
        e.setAllowAutoReply(c.isAllowAutoReply());
        e.setSystemPrompt(c.getSystemPrompt());
        e.setModel(c.getModel());
        e.setTemperature(c.getTemperature());
        e.setMaxTokens(c.getMaxTokens());
        e.setCooldownMinutes(c.getCooldownMinutes());
        e.setMaxChars(c.getMaxChars());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    private static AgentConfig toDomain(AgentConfigJpaEntity e) {
        return AgentConfig.reconstitute(e.getId(), e.getCompanyId(), e.isAiEnabled(),
                e.isAllowAutoReply(), e.getSystemPrompt(), e.getModel(), e.getTemperature(),
                e.getMaxTokens(), e.getCooldownMinutes(), e.getMaxChars(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}