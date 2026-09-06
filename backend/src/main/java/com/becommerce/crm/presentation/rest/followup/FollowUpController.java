package com.becommerce.crm.presentation.rest.followup;

import com.becommerce.crm.application.followup.dto.FollowUpRequest;
import com.becommerce.crm.application.followup.dto.FollowUpResponse;
import com.becommerce.crm.application.followup.port.input.FollowUpUseCase;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * FollowUps de conversas omnichannel (Sprint 22). Scoped à empresa ativa
 * (CurrentUser + RLS): o {@code companyId} vem do principal autenticado, nunca
 * do body. Leitura exige {@code omnichannel:followup:read}; agendar/cancelar
 * exigem {@code omnichannel:followup}.
 */
@RestController
@RequestMapping("/api/v1/omnichannel/follow-ups")
public class FollowUpController {

    private final FollowUpUseCase followUpUseCase;

    public FollowUpController(FollowUpUseCase followUpUseCase) {
        this.followUpUseCase = followUpUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('omnichannel:followup')")
    public ResponseEntity<FollowUpResponse> create(@Valid @RequestBody FollowUpRequest request,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        FollowUpResponse response = followUpUseCase.create(principal.companyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('omnichannel:followup:read')")
    public ResponseEntity<PageResponse<FollowUpResponse>> list(
            @RequestParam(required = false) UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(followUpUseCase.list(principal.companyId(), conversationId, page, pageSize));
    }

    @GetMapping("/{followUpId}")
    @PreAuthorize("hasAuthority('omnichannel:followup:read')")
    public ResponseEntity<FollowUpResponse> get(@PathVariable UUID followUpId,
                                                @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(followUpUseCase.get(principal.companyId(), followUpId));
    }

    @PostMapping("/{followUpId}/cancel")
    @PreAuthorize("hasAuthority('omnichannel:followup')")
    public ResponseEntity<FollowUpResponse> cancel(@PathVariable UUID followUpId,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(followUpUseCase.cancel(principal.companyId(), followUpId));
    }
}