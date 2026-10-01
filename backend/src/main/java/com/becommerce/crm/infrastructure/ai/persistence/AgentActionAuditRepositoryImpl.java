package com.becommerce.crm.infrastructure.ai.persistence;

import com.becommerce.crm.application.ai.port.output.AgentActionAuditRepository;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.domain.ai.AgentActionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AgentActionAuditRepositoryImpl implements AgentActionAuditRepository {

    private final AgentActionAuditJpaRepository jpaRepository;

    public AgentActionAuditRepositoryImpl(AgentActionAuditJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AgentActionAudit save(AgentActionAudit audit) {
        return toDomain(jpaRepository.save(toEntity(audit)));
    }

    @Override
    public PageResponse<AgentActionAudit> findByConversationId(UUID conversationId, int page, int size) {
        return toPage(jpaRepository.findByConversationIdOrderByExecutedAtDesc(
                conversationId, PageRequest.of(page, size)), page, size);
    }

    @Override
    public PageResponse<AgentActionAudit> findByCompanyId(UUID companyId, int page, int size) {
        return toPage(jpaRepository.findByCompanyIdOrderByExecutedAtDesc(
                companyId, PageRequest.of(page, size)), page, size);
    }

    private static PageResponse<AgentActionAudit> toPage(Page<AgentActionAuditJpaEntity> result,
                                                         int page, int size) {
        return PageResponse.of(result.getContent().stream()
                        .map(AgentActionAuditRepositoryImpl::toDomain)
                        .toList(),
                page, size, result.getTotalElements());
    }

    static AgentActionAudit toDomain(AgentActionAuditJpaEntity e) {
        return AgentActionAudit.reconstitute(e.getId(), e.getCompanyId(), e.getConversationId(),
                e.getToolName(), e.getInput(), e.getResult(), e.getExecutedBy(), e.getExecutedAt(),
                e.getOutcome());
    }

    static AgentActionAuditJpaEntity toEntity(AgentActionAudit audit) {
        AgentActionAuditJpaEntity e = new AgentActionAuditJpaEntity();
        e.setId(audit.getId());
        e.setCompanyId(audit.getCompanyId());
        e.setConversationId(audit.getConversationId());
        e.setToolName(audit.getToolName());
        e.setInput(audit.getInput());
        e.setResult(audit.getResult());
        e.setExecutedBy(audit.getExecutedBy());
        e.setExecutedAt(audit.getExecutedAt());
        e.setOutcome(audit.getOutcome());
        return e;
    }
}
