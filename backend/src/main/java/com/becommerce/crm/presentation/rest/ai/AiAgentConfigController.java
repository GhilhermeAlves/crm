package com.becommerce.crm.presentation.rest.ai;

import com.becommerce.crm.application.ai.dto.AgentConfigRequest;
import com.becommerce.crm.application.ai.dto.AgentConfigResponse;
import com.becommerce.crm.application.ai.port.input.AgentConfigUseCase;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin da configuração do agente de IA (Sprint 3). A empresa ativa é resolvida
 * do {@code CurrentUser} autenticado (JWT) — nunca do payload. Exige a permissão
 * {@code ai:agent-config}. GET devolve safe defaults se a empresa ainda não tiver
 * configuração; PUT cria/atualiza (upsert).
 */
@RestController
public class AiAgentConfigController {

    private final AgentConfigUseCase agentConfigUseCase;

    public AiAgentConfigController(AgentConfigUseCase agentConfigUseCase) {
        this.agentConfigUseCase = agentConfigUseCase;
    }

    @GetMapping("/api/v1/ai/agent-config")
    @PreAuthorize("hasAuthority('ai:agent-config')")
    public ResponseEntity<AgentConfigResponse> get(@AuthenticationPrincipal CurrentUser principal) {
        requireActiveCompany(principal);
        return ResponseEntity.ok(agentConfigUseCase.get(principal.companyId()));
    }

    @PutMapping("/api/v1/ai/agent-config")
    @PreAuthorize("hasAuthority('ai:agent-config')")
    public ResponseEntity<AgentConfigResponse> update(@Valid @RequestBody AgentConfigRequest request,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        requireActiveCompany(principal);
        return ResponseEntity.ok(agentConfigUseCase.update(principal.companyId(), request));
    }

    private static void requireActiveCompany(CurrentUser principal) {
        if (principal == null || principal.companyId() == null) {
            throw new CrmAccessDeniedException("Você precisa de uma empresa ativa para configurar o agente de IA.");
        }
    }
}