package com.becommerce.crm.clinical.budget.web;

import com.becommerce.crm.clinical.budget.application.dto.ApproveBudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetResponse;
import com.becommerce.crm.clinical.budget.application.dto.BudgetSummaryResponse;
import com.becommerce.crm.clinical.budget.application.port.in.BudgetUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Orçamentos do paciente (Fase 3b). */
@RestController
public class BudgetController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar orçamentos da sua própria empresa.";
    private static final String BASE = "/api/v1/companies/{companyId}/patients/{contactId}/budgets";

    private final BudgetUseCase useCase;

    public BudgetController(BudgetUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping(BASE)
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<BudgetSummaryResponse>> list(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId) {
        return ResponseEntity.ok(useCase.list(companyId, contactId));
    }

    @GetMapping(BASE + "/{budgetId}")
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<BudgetResponse> get(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID budgetId) {
        return ResponseEntity.ok(useCase.get(companyId, contactId, budgetId));
    }

    @PostMapping(BASE)
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<BudgetResponse> create(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, contactId, request, principal.userId()));
    }

    @PutMapping(BASE + "/{budgetId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<BudgetResponse> update(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.update(companyId, contactId, budgetId, request, principal.userId()));
    }

    @PostMapping(BASE + "/{budgetId}/approve")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<BudgetResponse> approve(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody ApproveBudgetRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.approve(companyId, contactId, budgetId, request,
                principal.userId(), canCreateFinance(principal)));
    }

    @PostMapping(BASE + "/{budgetId}/lose")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<BudgetResponse> lose(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(useCase.lose(companyId, contactId, budgetId, principal.userId()));
    }

    @DeleteMapping(BASE + "/{budgetId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID contactId,
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal CurrentUser principal) {
        useCase.delete(companyId, contactId, budgetId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    private static boolean canCreateFinance(CurrentUser p) {
        return p.permissions() != null && p.permissions().contains("finance:create");
    }
}
