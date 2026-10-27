package com.becommerce.crm.clinical.budget.title;

import com.becommerce.crm.clinical.budget.title.BudgetTitleDtos.BudgetTitleRequest;
import com.becommerce.crm.clinical.budget.title.BudgetTitleDtos.BudgetTitleResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Títulos de orçamento salvos da empresa (mesmas permissões dos orçamentos). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/budget-titles")
public class BudgetTitleController {

    private static final String ACCESS_MESSAGE =
            "Você só pode acessar títulos de orçamento da sua própria empresa.";

    private final BudgetTitleService service;

    public BudgetTitleController(BudgetTitleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contact:read')")
    public ResponseEntity<List<BudgetTitleResponse>> list(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId) {
        return ResponseEntity.ok(service.list(companyId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<BudgetTitleResponse> create(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @Valid @RequestBody BudgetTitleRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(companyId, request.title(), principal.userId()));
    }

    @DeleteMapping("/{titleId}")
    @PreAuthorize("hasAuthority('contact:update')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(ACCESS_MESSAGE) UUID companyId,
            @PathVariable UUID titleId) {
        service.delete(companyId, titleId);
        return ResponseEntity.noContent().build();
    }
}
