package com.becommerce.crm.finance.accounting.web;

import com.becommerce.crm.finance.accounting.application.dto.AccountingReportRequestDto;
import com.becommerce.crm.finance.accounting.application.service.AccountingReportService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Relatórios contábeis (Etapa 3): {@code POST /finance/accounting-reports}
 * gera o arquivo PDF/CSV (exige {@code finance:read}) e
 * {@code GET /available-months} lista os meses com movimento para o seletor.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/finance/accounting-reports")
public class AccountingReportController {

    private static final String OWN_COMPANY =
            "Você só pode acessar os relatórios financeiros da sua própria empresa.";

    private final AccountingReportService service;

    public AccountingReportController(AccountingReportService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<byte[]> generate(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestBody AccountingReportRequestDto request,
            @AuthenticationPrincipal CurrentUser principal) {
        byte[] content = service.generate(companyId, request, principal.userId());
        String fileName = service.fileName(request);
        MediaType mediaType = fileName.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(content);
    }

    @GetMapping("/available-months")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<List<String>> availableMonths(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(service.availableMonths(companyId));
    }
}