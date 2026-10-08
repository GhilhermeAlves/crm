package com.becommerce.crm.automation.ai.web;

import com.becommerce.crm.automation.ai.application.dto.AgentMemoryRequest;
import com.becommerce.crm.automation.ai.application.dto.AgentMemoryResponse;
import com.becommerce.crm.automation.ai.application.memory.AgentMemoryAdminService;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Memórias do agente por contato. Empresa sempre do {@code CurrentUser} (JWT),
 * nunca do payload; exige {@code ai:agent-config} (mesma permissão do agente).
 */
@RestController
public class AiAgentMemoryController {

    private final AgentMemoryAdminService memoryAdminService;

    public AiAgentMemoryController(AgentMemoryAdminService memoryAdminService) {
        this.memoryAdminService = memoryAdminService;
    }

    @GetMapping("/api/v1/ai/agent-memories")
    @PreAuthorize("hasAuthority('ai:agent-config')")
    public ResponseEntity<List<AgentMemoryResponse>> list(@RequestParam UUID contactId,
                                                          @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(memoryAdminService.list(companyId(principal), contactId));
    }

    @PostMapping("/api/v1/ai/agent-memories")
    @PreAuthorize("hasAuthority('ai:agent-config')")
    public ResponseEntity<AgentMemoryResponse> create(@Valid @RequestBody AgentMemoryRequest request,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoryAdminService.create(companyId(principal), request));
    }

    @PutMapping("/api/v1/ai/agent-memories/{id}")
    @PreAuthorize("hasAuthority('ai:agent-config')")
    public ResponseEntity<AgentMemoryResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody AgentMemoryRequest request,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        return memoryAdminService.update(companyId(principal), id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/v1/ai/agent-memories/{id}")
    @PreAuthorize("hasAuthority('ai:agent-config')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser principal) {
        return memoryAdminService.delete(companyId(principal), id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AgentMemoryAdminService.MemoryRequestException.class)
    public ResponseEntity<Map<String, String>> handleRequestError(AgentMemoryAdminService.MemoryRequestException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    private static UUID companyId(CurrentUser principal) {
        if (principal == null || principal.companyId() == null) {
            throw new CrmAccessDeniedException("Você precisa de uma empresa ativa para gerenciar memórias do agente.");
        }
        return principal.companyId();
    }
}
