package com.becommerce.crm.application.ai.dto;

import java.util.List;

/**
 * Contrato da análise contextual (AI-06). Separa inequivocamente resumo,
 * fatos, inferências e recomendações. Os {@code facts} são sempre derivados de
 * dado real do CRM pelo backend; {@code inferences}, {@code recommendations} e
 * {@code summary} podem ser produzidos pelo modelo, mas permanecem
 * estruturalmente distintos dos fatos.
 */
public record AiAnalysisResponse(
        String summary,
        List<AiFact> facts,
        List<AiInference> inferences,
        List<AiRecommendation> recommendations
) {

    public AiAnalysisResponse {
        facts = facts == null ? List.of() : List.copyOf(facts);
        inferences = inferences == null ? List.of() : List.copyOf(inferences);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}