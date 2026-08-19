package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiConversationResponse;
import com.becommerce.crm.application.ai.dto.AiMessageResponse;
import com.becommerce.crm.application.ai.port.input.AiAssistantUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Hist�rico de conversas do assistente de IA (AI-04). Permite listar as
 * conversas do usu�rio e retomar uma conversa exibindo suas mensagens. Exige a
 * permiss�o {@code ai:chat}. A propriedade � sempre resolvida do usu�rio
 * autenticado (empresa + usu�rio) - nunca do cliente; conversa de outro
 * usu�rio/empresa resulta em 404.
 */
@RestController
public class AiConversationHistoryController {

    private final AiAssistantUseCase aiAssistantUseCase;

    public AiConversationHistoryController(AiAssistantUseCase aiAssistantUseCase) {
        this.aiAssistantUseCase = aiAssistantUseCase;
    }

    @GetMapping("/api/v1/ai/conversations")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<List<AiConversationResponse>> listConversations(
            @AuthenticationPrincipal CurrentUser principal) {
        if (principal.companyId() == null) {
            throw new CrmAccessDeniedException("Voc� precisa de uma empresa ativa para usar a IA.");
        }
        List<AiConversationResponse> conversations =
                aiAssistantUseCase.listConversations(principal.companyId(), principal.userId());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/api/v1/ai/conversations/{conversationId}/messages")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<List<AiMessageResponse>> listMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal CurrentUser principal) {
        if (principal.companyId() == null) {
            throw new CrmAccessDeniedException("Voc� precisa de uma empresa ativa para usar a IA.");
        }
        List<AiMessageResponse> messages =
                aiAssistantUseCase.getConversationMessages(principal.companyId(), principal.userId(), conversationId);
        return ResponseEntity.ok(messages);
    }
}