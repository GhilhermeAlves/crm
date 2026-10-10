package com.becommerce.crm.masterdata.anamnesis.application.port.output;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestion;

import java.util.List;
import java.util.UUID;

/** Porta de saída das perguntas de anamnese ({@code anamnesis_questions}, V091). */
public interface AnamnesisQuestionRepository {

    AnamnesisQuestion save(AnamnesisQuestion question);

    List<AnamnesisQuestion> findByModelIdOrderBySortOrder(UUID modelId);

    void deleteByModelId(UUID modelId);
}
