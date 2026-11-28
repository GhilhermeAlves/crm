package com.becommerce.crm.clinical.trichology.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Anamnese concluída vista pelo exame tricológico: respostas vinculadas no formato de chaves
 * das regras do Copiloto ({@code quizLike}) e perguntas/respostas legíveis para a IA do laudo,
 * sem dados que identifiquem a pessoa.
 */
public interface AnamnesisClinicalView {

    record QuestionAnswer(String section, String question, String answer) {
    }

    record ClinicalAnamnesis(UUID anamnesisId, LocalDateTime completedAt, Map<String, Object> quizLike,
                             List<QuestionAnswer> answers) {
    }

    Optional<ClinicalAnamnesis> latestCompleted(UUID companyId, UUID contactId);

    Optional<ClinicalAnamnesis> byId(UUID companyId, UUID contactId, UUID anamnesisId);
}
