package com.becommerce.crm.presentation.rest.followup;

import com.becommerce.crm.application.followup.dto.FollowUpSequenceRequest;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceResponse;
import com.becommerce.crm.application.followup.port.input.FollowUpSequenceUseCase;
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
 * Sequências de follow-up (Sprint 22). Scoped à empresa ativa (CurrentUser +
 * RLS). CRUD restrito a ADMIN/MANAGER:
 * {@code omnichannel:followup:sequence} (manage) e
 * {@code omnichannel:followup:sequence:read} (read).
 */
@RestController
@RequestMapping("/api/v1/omnichannel/follow-up-sequences")
public class FollowUpSequenceController {

    private final FollowUpSequenceUseCase sequenceUseCase;

    public FollowUpSequenceController(FollowUpSequenceUseCase sequenceUseCase) {
        this.sequenceUseCase = sequenceUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence')")
    public ResponseEntity<FollowUpSequenceResponse> create(
            @Valid @RequestBody FollowUpSequenceRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        FollowUpSequenceResponse response = sequenceUseCase.create(principal.companyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence:read')")
    public ResponseEntity<PageResponse<FollowUpSequenceResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(sequenceUseCase.list(principal.companyId(), page, pageSize));
    }

    @GetMapping("/{sequenceId}")
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence:read')")
    public ResponseEntity<FollowUpSequenceResponse> get(
            @PathVariable UUID sequenceId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(sequenceUseCase.get(principal.companyId(), sequenceId));
    }

    @PutMapping("/{sequenceId}")
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence')")
    public ResponseEntity<FollowUpSequenceResponse> update(
            @PathVariable UUID sequenceId,
            @Valid @RequestBody FollowUpSequenceRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(sequenceUseCase.update(principal.companyId(), sequenceId, request));
    }

    @DeleteMapping("/{sequenceId}")
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID sequenceId,
            @AuthenticationPrincipal CurrentUser principal) {
        sequenceUseCase.delete(principal.companyId(), sequenceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sequenceId}/activate")
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence')")
    public ResponseEntity<FollowUpSequenceResponse> activate(
            @PathVariable UUID sequenceId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(sequenceUseCase.activate(principal.companyId(), sequenceId));
    }

    @PostMapping("/{sequenceId}/deactivate")
    @PreAuthorize("hasAuthority('omnichannel:followup:sequence')")
    public ResponseEntity<FollowUpSequenceResponse> deactivate(
            @PathVariable UUID sequenceId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(sequenceUseCase.deactivate(principal.companyId(), sequenceId));
    }
}