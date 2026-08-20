package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiAnalysisRequest;
import com.becommerce.crm.application.ai.dto.AiAnalysisResponse;
import com.becommerce.crm.application.ai.port.input.AiContextualAnalysisUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Análise contextual (AI-06). A identidade (empresa/usuário/permissões) é
 * derivada do {@code CurrentUser} autenticado (JWT) — nunca do payload. Exige a
 * mesma permissão de chat ({@code ai:chat}) e respeita TenantContext/RLS.
 * Nenhuma ação é executada: a resposta contém apenas fatos, inferências,
 * recomendações e resumo.
 */
@RestController
public class AiAnalysisController {

    private final AiContextualAnalysisUseCase analysisUseCase;

    public AiAnalysisController(AiContextualAnalysisUseCase analysisUseCase) {
        this.analysisUseCase = analysisUseCase;
    }

    @PostMapping("/api/v1/ai/analyze")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<AiAnalysisResponse> analyze(@RequestBody AiAnalysisRequest request,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        if (principal.companyId() == null) {
            throw new CrmAccessDeniedException("Você precisa de uma empresa ativa para usar a IA.");
        }
        AiAnalysisResponse response = analysisUseCase.analyze(principal.companyId(),
                principal.userId(), principal.permissions(), request);
        return ResponseEntity.ok(response);
    }
}