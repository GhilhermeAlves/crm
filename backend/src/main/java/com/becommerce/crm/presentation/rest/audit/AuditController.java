package com.becommerce.crm.presentation.rest.audit;

import com.becommerce.crm.application.audit.dto.AuditLogPageResponse;
import com.becommerce.crm.application.audit.dto.AuditLogResponse;
import com.becommerce.crm.application.audit.dto.AuditLogSearchRequest;
import com.becommerce.crm.application.audit.port.input.AuditUseCase;
import com.becommerce.crm.infrastructure.security.filter.JwtUserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditUseCase auditUseCase;

    public AuditController(AuditUseCase auditUseCase) {
        this.auditUseCase = auditUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ResponseEntity<AuditLogPageResponse> search(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        AuditLogSearchRequest request = new AuditLogSearchRequest(
            page, pageSize, module, action, status, userId,
            entityId, entityName, search, startDate, endDate
        );

        return ResponseEntity.ok(AuditLogPageResponse.of(auditUseCase.search(principal.companyId(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('audit:read')")
    public ResponseEntity<AuditLogResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(auditUseCase.getById(id, principal.companyId()));
    }
}
