package com.becommerce.crm.finance.report.web;

import com.becommerce.crm.finance.report.application.dto.AgendaReportResponse;
import com.becommerce.crm.finance.report.application.dto.CashFlowResponse;
import com.becommerce.crm.finance.report.application.dto.HeadlineResponse;
import com.becommerce.crm.finance.report.application.dto.HealthResponse;
import com.becommerce.crm.finance.report.application.dto.OverviewResponse;
import com.becommerce.crm.finance.report.application.dto.RevenueDistributionResponse;
import com.becommerce.crm.finance.report.application.dto.SummaryResponse;
import com.becommerce.crm.finance.report.application.service.FinanceReportService;
import com.becommerce.crm.finance.report.domain.ReportGranularity;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Relatórios financeiros (Etapa 2, somente leitura) — consultas SQL nativas de
 * leitura, sem migrations. Respostas sempre em centavos; status CANCELLED nunca
 * entra em soma. /overview e /agenda-report leem a agenda, então exigem
 * {@code appointment:read}; os demais exigem {@code finance:read}.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/finance")
public class FinanceReportController {

    private static final String OWN_COMPANY =
            "Você só pode acessar os relatórios financeiros da sua própria empresa.";

    private final FinanceReportService service;

    public FinanceReportController(FinanceReportService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<SummaryResponse> summary(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) UUID professionalUserId,
            @RequestParam(required = false) UUID contactId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.summary(companyId, type, status, from, to, paymentMethod,
                category, professionalUserId, contactId, q));
    }

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<HealthResponse> health(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(service.health(companyId, months));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<CashFlowResponse> cashFlow(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "MONTH") ReportGranularity granularity) {
        return ResponseEntity.ok(service.cashFlow(companyId, granularity, from, to));
    }

    @GetMapping("/revenue-distribution")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<RevenueDistributionResponse> revenueDistribution(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "MONTH") ReportGranularity granularity,
            @RequestParam(required = false) List<UUID> catalogItemIds) {
        return ResponseEntity.ok(service.revenueDistribution(companyId, granularity, from, to,
                catalogItemIds));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<OverviewResponse> overview(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "MONTH") ReportGranularity granularity,
            @RequestParam(required = false) List<UUID> professionalUserIds) {
        return ResponseEntity.ok(service.overview(companyId, granularity, from, to,
                professionalUserIds));
    }

    @GetMapping("/agenda-report")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<AgendaReportResponse> agendaReport(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "MONTH") ReportGranularity granularity,
            @RequestParam(required = false) UUID appointmentType,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<UUID> professionalUserIds) {
        // Granularity é parte do contrato da API, mas o agregado do período
        // independe do agrupamento (a resposta não traz série temporal).
        return ResponseEntity.ok(
                service.agendaReport(companyId, from, to, appointmentType, statuses,
                        professionalUserIds));
    }

    @GetMapping("/headline")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<HeadlineResponse> headline(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        return ResponseEntity.ok(service.headline(companyId));
    }
}