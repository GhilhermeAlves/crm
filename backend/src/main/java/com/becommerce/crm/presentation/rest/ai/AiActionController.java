package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.input.AiActionUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Acoes de escrita do assistente de IA (AI-05). Confirmar executa uma proposta
 * persistida; cancelar recusa uma proposta pendente; listar retorna as acoes de
 * uma conversa para reconstrucao de historico. Exige {@code ai:chat}; a
 * permissao de negocio da ferramenta e verificada no service de confirmacao. A
 * propriedade (empresa + usuario) e sempre resolvida do {@code CurrentUser}
 * autenticado - nunca do payload.
 */
@RestController
public class AiActionController {

    private final AiActionUseCase aiActionUseCase;

    public AiActionController(AiActionUseCase aiActionUseCase) {
        this.aiActionUseCase = aiActionUseCase;
    }

    @PostMapping("/api/v1/ai/actions/{actionId}/confirm")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<AiActionResponse> confirm(
            @PathVariable UUID actionId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompany(principal);
        AiActionResponse response = aiActionUseCase.confirm(
                principal.companyId(), principal.userId(), principal.permissions(), actionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/ai/actions/{actionId}/cancel")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<AiActionResponse> cancel(
            @PathVariable UUID actionId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompany(principal);
        AiActionResponse response = aiActionUseCase.cancel(
                principal.companyId(), principal.userId(), actionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/ai/conversations/{conversationId}/actions")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ResponseEntity<List<AiActionResponse>> listByConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal CurrentUser principal) {
        requireCompany(principal);
        List<AiActionResponse> actions = aiActionUseCase.listByConversation(
                principal.companyId(), principal.userId(), conversationId);
        return ResponseEntity.ok(actions);
    }

    private void requireCompany(CurrentUser principal) {
        if (principal.companyId() == null) {
            throw new CrmAccessDeniedException("Voce precisa de uma empresa ativa para usar a IA.");
        }
    }
}