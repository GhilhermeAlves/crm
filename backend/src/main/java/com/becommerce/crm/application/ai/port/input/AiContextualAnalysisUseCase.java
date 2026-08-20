package com.becommerce.crm.application.ai.port.input;

import com.becommerce.crm.application.ai.dto.AiAnalysisRequest;
import com.becommerce.crm.application.ai.dto.AiAnalysisResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso da análise contextual (AI-06). Produz uma análise (fatos,
 * inferências, recomendações e resumo) a partir do contexto CRM real. A
 * identidade (empresa/usuário/permissões) vem do {@code CurrentUser} — nunca do
 * payload. NUNCA executa ações: qualquer recomendação é apenas sugestão.
 */
public interface AiContextualAnalysisUseCase {

    AiAnalysisResponse analyze(UUID companyId, UUID userId, List<String> permissions,
                               AiAnalysisRequest request);
}