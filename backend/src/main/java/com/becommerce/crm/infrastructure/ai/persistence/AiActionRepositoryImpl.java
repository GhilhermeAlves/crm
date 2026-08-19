package com.becommerce.crm.infrastructure.ai.persistence;

import com.becommerce.crm.application.ai.port.output.AiActionRepository;
import com.becommerce.crm.domain.ai.AiAction;
import com.becommerce.crm.domain.ai.AiActionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AiActionRepositoryImpl implements AiActionRepository {

    private final AiActionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public AiActionRepositoryImpl(AiActionJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAction save(AiAction action) {
        return toDomain(jpaRepository.save(toEntity(action)));
    }

    @Override
    public Optional<AiAction> findById(UUID id) {
        return jpaRepository.findById(id).map(AiActionRepositoryImpl::toDomain);
    }

    @Override
    public Optional<AiAction> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(AiActionRepositoryImpl::toDomain);
    }

    @Override
    public List<AiAction> findByConversationId(UUID conversationId) {
        return jpaRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(AiActionRepositoryImpl::toDomain)
                .toList();
    }

    private AiActionJpaEntity toEntity(AiAction a) {
        AiActionJpaEntity e = new AiActionJpaEntity();
        e.setId(a.getId());
        e.setCompanyId(a.getCompanyId());
        e.setUserId(a.getUserId());
        e.setConversationId(a.getConversationId());
        e.setTool(a.getTool());
        e.setEntityType(a.getEntityType());
        e.setEntityId(a.getEntityId());
        e.setDescription(a.getDescription());
        e.setParameters(a.getParameters());
        e.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        e.setResult(toMap(a.getResult()));
        e.setErrorMessage(a.getErrorMessage());
        e.setVersion(a.getVersion());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    private static AiAction toDomain(AiActionJpaEntity e) {
        AiActionStatus status = e.getStatus() != null ? AiActionStatus.valueOf(e.getStatus()) : null;
        return AiAction.reconstitute(
                e.getId(), e.getCompanyId(), e.getUserId(), e.getConversationId(),
                e.getTool(), e.getEntityType(), e.getEntityId(), e.getParameters(),
                e.getDescription(), status, e.getResult(), e.getErrorMessage(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() { });
        } catch (Exception ex) {
            return Map.of("value", String.valueOf(value));
        }
    }
}