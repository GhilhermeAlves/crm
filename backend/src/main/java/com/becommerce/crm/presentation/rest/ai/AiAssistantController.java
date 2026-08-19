package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiChatRequest;
import com.becommerce.crm.application.ai.dto.AiChatResponse;
import com.becommerce.crm.application.ai.port.input.AiAssistantUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat do assistente de IA (AI-01). O usuário e a empresa ativa são resolvidos
 * do {@code CurrentUser} autenticado (JWT) — nunca do payload. Exige a
 * permissão {@code ai:chat}.
 */
@RestController
public class AiAssistantController {

    private final AiAssistantUseCase aiAssistantUseCase;

    public AiAssistantController(AiAssistantUseCase aiAssistantUseCase) {
        this.aiAssistantUseCase = aiAssistantUseCase;
    }

    @PostMapping("/api/v1/ai/chat")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request,
                                               @AuthenticationPrincipal CurrentUser principal) {
        if (principal.companyId() == null) {
            throw new CrmAccessDeniedException("Você precisa de uma empresa ativa para usar a IA.");
        }
        AiChatResponse response = aiAssistantUseCase.chat(principal.companyId(), principal.userId(),
                principal.permissions(), request);
        return ResponseEntity.ok(response);
    }
}
