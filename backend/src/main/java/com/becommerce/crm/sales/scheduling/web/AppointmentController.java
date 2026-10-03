package com.becommerce.crm.sales.scheduling.web;

import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
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
public class AppointmentController {

    private final AppointmentUseCase useCase;

    public AppointmentController(AppointmentUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/api/v1/companies/{companyId}/appointments")
    @PreAuthorize("hasAuthority('appointment:create')")
    public ResponseEntity<AppointmentResponse> create(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(useCase.create(companyId, request, principal.userId()));
    }

    @GetMapping("/api/v1/companies/{companyId}/appointments")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<AppointmentResponse>> list(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) List<UUID> hostIds) {
        return ResponseEntity.ok(useCase.list(companyId, from, to, hostIds));
    }

    @GetMapping("/api/v1/companies/{companyId}/appointments/{appointmentId}")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<AppointmentResponse> getById(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID appointmentId) {
        return ResponseEntity.ok(useCase.getById(companyId, appointmentId));
    }

    @PutMapping("/api/v1/companies/{companyId}/appointments/{appointmentId}")
    @PreAuthorize("hasAuthority('appointment:update')")
    public ResponseEntity<AppointmentResponse> update(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return ResponseEntity.ok(useCase.update(companyId, appointmentId, request));
    }

    @PatchMapping("/api/v1/companies/{companyId}/appointments/{appointmentId}/reschedule")
    @PreAuthorize("hasAuthority('appointment:update')")
    public ResponseEntity<AppointmentResponse> reschedule(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(useCase.reschedule(companyId, appointmentId, request));
    }

    @PostMapping("/api/v1/companies/{companyId}/appointments/{appointmentId}/status/{status}")
    @PreAuthorize("hasAuthority('appointment:update')")
    public ResponseEntity<AppointmentResponse> changeStatus(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID appointmentId,
            @PathVariable AppointmentStatus status) {
        return ResponseEntity.ok(useCase.changeStatus(companyId, appointmentId, status));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/appointments/{appointmentId}")
    @PreAuthorize("hasAuthority('appointment:delete')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID appointmentId) {
        useCase.delete(companyId, appointmentId);
        return ResponseEntity.noContent().build();
    }
}
