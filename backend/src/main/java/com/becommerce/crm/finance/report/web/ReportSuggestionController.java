package com.becommerce.crm.finance.report.web;

import com.becommerce.crm.finance.report.application.dto.CreateReportSuggestionRequest;
import com.becommerce.crm.finance.report.application.dto.ReportSuggestionResponse;
import com.becommerce.crm.finance.report.application.service.ReportSuggestionService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Sugestões de relatório (V094). Isolamento por RLS FORCE + {@code @CurrentCompanyId}.
 * Qualquer membro autenticado da empresa pode sugerir — sem authority específica.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/report-suggestions")
public class ReportSuggestionController {

    private static final String OWN_COMPANY =
            "Você só pode enviar sugestões para a sua própria empresa.";

    private final ReportSuggestionService service;

    public ReportSuggestionController(ReportSuggestionService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportSuggestionResponse> create(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @Valid @RequestBody CreateReportSuggestionRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(companyId, request, principal.userId()));
    }
}
