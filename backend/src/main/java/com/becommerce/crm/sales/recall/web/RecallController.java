package com.becommerce.crm.sales.recall.web;

import com.becommerce.crm.sales.recall.application.dto.CreateRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.dto.RecallSuggestionResponse;
import com.becommerce.crm.sales.recall.application.dto.ScheduleRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.UpdateRecallRequest;
import com.becommerce.crm.sales.recall.application.port.in.RecallUseCase;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/recalls")
public class RecallController {

    private final RecallUseCase useCase;

    public RecallController(RecallUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<PageResponse<RecallResponse>> list(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "NEXT_7_DAYS") String window,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(useCase.list(companyId, hostId, tag, q, window, page, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('appointment:create')")
    public ResponseEntity<RecallResponse> create(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateRecallRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, request, principal.userId()));
    }

    @PatchMapping("/{recallId}")
    @PreAuthorize("hasAuthority('appointment:update')")
    public ResponseEntity<RecallResponse> update(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @PathVariable UUID recallId,
            @Valid @RequestBody UpdateRecallRequest request) {
        return ResponseEntity.ok(useCase.update(companyId, recallId, request));
    }

    @PostMapping("/{recallId}/dismiss")
    @PreAuthorize("hasAuthority('appointment:update')")
    public ResponseEntity<RecallResponse> dismiss(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @PathVariable UUID recallId) {
        return ResponseEntity.ok(useCase.dismiss(companyId, recallId));
    }

    @PostMapping("/{recallId}/schedule")
    @PreAuthorize("hasAuthority('appointment:create')")
    public ResponseEntity<RecallResponse> schedule(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @PathVariable UUID recallId,
            @Valid @RequestBody ScheduleRecallRequest request) {
        return ResponseEntity.ok(useCase.schedule(companyId, recallId, request));
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<com.becommerce.crm.shared.application.dto.PageResponse<RecallSuggestionResponse>> suggestions(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @RequestParam(defaultValue = "3") int since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(useCase.suggestions(companyId, since, page, pageSize));
    }

    @PostMapping("/suggestions/{contactId}/dismiss")
    @PreAuthorize("hasAuthority('appointment:update')")
    public ResponseEntity<Void> dismissSuggestion(
            @CurrentCompanyId("Você só pode acessar retornos da sua própria empresa.") UUID companyId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.dismissSuggestion(companyId, contactId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
