package com.becommerce.crm.reports.export.web;

import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.reports.export.application.dto.ReportExportRequest;
import com.becommerce.crm.reports.export.application.dto.ReportExportResponse;
import com.becommerce.crm.reports.export.application.service.ReportExportService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Impressão/exportação de relatórios: o {@code POST} agenda a geração (202) e o
 * usuário é avisado no sino; {@code GET /{id}/download} entrega o arquivo. A
 * permissão fina (finance:read / appointment:read) é checada no service.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/report-exports")
public class ReportExportController {

    private static final String OWN_COMPANY = "Você só pode exportar relatórios da sua própria empresa.";

    private final ReportExportService service;

    public ReportExportController(ReportExportService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportExportResponse> request(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                        @RequestBody ReportExportRequest request,
                                                        @AuthenticationPrincipal CurrentUser principal) {
        ReportExportResponse response = service.request(companyId, principal.userId(), principal.permissions(),
                request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReportExportResponse>> recent(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                             @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.recent(companyId, principal.userId()));
    }

    @GetMapping("/{exportId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportExportResponse> get(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                    @PathVariable UUID exportId,
                                                    @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(service.get(companyId, principal.userId(), exportId));
    }

    @GetMapping("/{exportId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                           @PathVariable UUID exportId,
                                           @AuthenticationPrincipal CurrentUser principal) {
        StorageDownload file = service.download(companyId, principal.userId(), principal.permissions(), exportId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.data().length)
                .body(file.data());
    }
}
