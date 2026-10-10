package com.becommerce.crm.masterdata.anamnesis.application.port.output;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionOption;

import java.util.List;
import java.util.UUID;

/** Porta de saída das opções de resposta ({@code anamnesis_question_options}, V091). */
public interface AnamnesisQuestionOptionRepository {

    AnamnesisQuestionOption save(AnamnesisQuestionOption option);

    /** Opções de todas as perguntas de um modelo, ordenadas por pergunta e posição. */
    List<AnamnesisQuestionOption> findByModelIdOrderBySortOrder(UUID modelId);
}
