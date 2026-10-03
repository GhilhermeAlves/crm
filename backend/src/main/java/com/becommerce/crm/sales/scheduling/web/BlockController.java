package com.becommerce.crm.sales.scheduling.web;

import com.becommerce.crm.sales.scheduling.application.dto.BlockResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateBlockRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.BlockUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class BlockController {

    private final BlockUseCase useCase;

    public BlockController(BlockUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/api/v1/companies/{companyId}/blocks")
    @PreAuthorize("hasAuthority('appointment:create')")
    public ResponseEntity<BlockResponse> create(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateBlockRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, request, principal.userId()));
    }

    @GetMapping("/api/v1/companies/{companyId}/blocks")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<BlockResponse>> list(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) List<UUID> hostIds) {
        return ResponseEntity.ok(useCase.list(companyId, from, to, hostIds));
    }

    @GetMapping("/api/v1/companies/{companyId}/blocks/{blockId}")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<BlockResponse> getById(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID blockId) {
        return ResponseEntity.ok(useCase.getById(companyId, blockId));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/blocks/{blockId}")
    @PreAuthorize("hasAuthority('appointment:delete')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID blockId) {
        useCase.delete(companyId, blockId);
        return ResponseEntity.noContent().build();
    }
}
