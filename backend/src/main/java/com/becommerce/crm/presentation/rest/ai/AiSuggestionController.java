package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiSuggestionResponse;
import com.becommerce.crm.application.ai.port.input.AiSuggestionUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Sugestão de resposta com IA (Sprint 20, Módulo de IA). Gera uma resposta
 * sugerida para uma conversa omnichannel com base no histórico de mensagens.
 */
@RestController
public class AiSuggestionController {

    private final AiSuggestionUseCase aiSuggestionUseCase;

    public AiSuggestionController(AiSuggestionUseCase aiSuggestionUseCase) {
        this.aiSuggestionUseCase = aiSuggestionUseCase;
    }

    @GetMapping("/api/v1/ai/suggestions/{conversationId}")
    @PreAuthorize("hasAuthority('ai:suggest')")
    public ResponseEntity<AiSuggestionResponse> suggest(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal CurrentUser principal) {
        if (principal.companyId() == null) {
            throw new CrmAccessDeniedException("Você precisa de uma empresa ativa para usar a IA.");
        }
        AiSuggestionResponse response = aiSuggestionUseCase.suggest(principal.companyId(), conversationId);
        return ResponseEntity.ok(response);
    }
}