package com.becommerce.crm.masterdata.anamnesis.infrastructure.persistence;

import com.becommerce.crm.masterdata.anamnesis.application.port.output.AnamnesisQuestionRepository;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisComplementTrigger;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestion;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AnamnesisQuestionRepositoryImpl implements AnamnesisQuestionRepository {

    private final AnamnesisQuestionJpaRepository jpaRepository;

    public AnamnesisQuestionRepositoryImpl(AnamnesisQuestionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AnamnesisQuestion save(AnamnesisQuestion question) {
        return toDomain(jpaRepository.save(toEntity(question)));
    }

    @Override
    public List<AnamnesisQuestion> findByModelIdOrderBySortOrder(UUID modelId) {
        return jpaRepository.findByModelIdOrderBySortOrderAsc(modelId).stream()
                .map(AnamnesisQuestionRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public void deleteByModelId(UUID modelId) {
        jpaRepository.deleteByModelId(modelId);
    }

    static AnamnesisQuestion toDomain(AnamnesisQuestionJpaEntity e) {
        return AnamnesisQuestion.reconstitute(e.getId(), e.getSectionId(), e.getModelId(), e.getCompanyId(),
                e.getText(), AnamnesisQuestionType.valueOf(e.getQuestionType()), e.isRequired(),
                e.isHighlight(), e.isAllowComplement(), e.getComplementLabel(),
                AnamnesisComplementTrigger.valueOf(e.getComplementTrigger()), e.getComplementOptionValue(),
                e.getSortOrder(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static AnamnesisQuestionJpaEntity toEntity(AnamnesisQuestion q) {
        AnamnesisQuestionJpaEntity e = new AnamnesisQuestionJpaEntity();
        e.setId(q.getId());
        e.setSectionId(q.getSectionId());
        e.setModelId(q.getModelId());
        e.setCompanyId(q.getCompanyId());
        e.setText(q.getText());
        e.setQuestionType(q.getType().name());
        e.setRequired(q.isRequired());
        e.setHighlight(q.isHighlight());
        e.setAllowComplement(q.isAllowComplement());
        e.setComplementLabel(q.getComplementLabel());
        e.setComplementTrigger(q.getComplementTrigger().name());
        e.setComplementOptionValue(q.getComplementOptionValue());
        e.setSortOrder(q.getSortOrder());
        e.setCreatedAt(q.getCreatedAt());
        e.setUpdatedAt(q.getUpdatedAt());
        return e;
    }
}
