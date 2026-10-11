package com.becommerce.crm.finance.transaction.web;

import com.becommerce.crm.finance.transaction.application.dto.CreateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.dto.PayTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.UpdateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lançamentos financeiros (V092). Isolamento por RLS FORCE + {@code @CurrentCompanyId}.
 * {@code status} aceita {@code PENDING}, {@code PAID}, {@code CANCELLED} ou o valor
 * calculado {@code OVERDUE} (pendente com vencimento passado).
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/finance/transactions")
public class FinancialTransactionController {

    private static final String OWN_COMPANY =
            "Você só pode acessar os lançamentos financeiros da sua própria empresa.";

    private final FinancialTransactionService service;

    public FinancialTransactionController(FinancialTransactionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<PageResponse<FinancialTransactionResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) UUID professionalUserId,
            @RequestParam(required = false) UUID contactId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(service.list(companyId, type, status, from, to, paymentMethod, category,
                professionalUserId, contactId, q, page, pageSize));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<FinancialTransactionResponse> getById(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId) {
        return ResponseEntity.ok(service.getById(companyId, transactionId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<List<FinancialTransactionResponse>> create(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @Valid @RequestBody CreateFinancialTransactionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(companyId, request, principal.userId()));
    }

    @PutMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<FinancialTransactionResponse> update(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateFinancialTransactionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.update(companyId, transactionId, request, principal.userId()));
    }

    @PostMapping("/{transactionId}/pay")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<FinancialTransactionResponse> pay(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId,
            @Valid @RequestBody PayTransactionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.pay(companyId, transactionId, request, principal.userId()));
    }

    @PostMapping("/{transactionId}/cancel")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<FinancialTransactionResponse> cancel(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.cancel(companyId, transactionId, principal.userId()));
    }

    @DeleteMapping("/{transactionId}")
    @PreAuthorize("hasAuthority('finance:manage')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID transactionId,
            @AuthenticationPrincipal CurrentUser principal) {
        service.delete(companyId, transactionId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
