package com.becommerce.crm.sales.scheduling.web;

import com.becommerce.crm.sales.scheduling.application.dto.*;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import com.becommerce.crm.sales.scheduling.application.service.SlotService;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class AppointmentTypeController {

    private final AppointmentTypeUseCase useCase;
    private final SlotService slotService;

    public AppointmentTypeController(AppointmentTypeUseCase useCase, SlotService slotService) {
        this.useCase = useCase;
        this.slotService = slotService;
    }

    @PostMapping("/api/v1/companies/{companyId}/appointment-types")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<AppointmentTypeResponse> create(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateAppointmentTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(companyId, request));
    }

    @GetMapping("/api/v1/companies/{companyId}/appointment-types")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<AppointmentTypeResponse>> list(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(useCase.listByCompany(companyId));
    }

    @GetMapping("/api/v1/companies/{companyId}/appointment-types/{typeId}")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<AppointmentTypeResponse> getById(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID typeId) {
        return ResponseEntity.ok(useCase.getById(companyId, typeId));
    }

    @PutMapping("/api/v1/companies/{companyId}/appointment-types/{typeId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<AppointmentTypeResponse> update(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID typeId,
            @Valid @RequestBody UpdateAppointmentTypeRequest request) {
        return ResponseEntity.ok(useCase.update(companyId, typeId, request));
    }

    @DeleteMapping("/api/v1/companies/{companyId}/appointment-types/{typeId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID typeId) {
        useCase.delete(companyId, typeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/companies/{companyId}/appointment-types/{typeId}/slots")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID typeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(slotService.getAvailableSlots(companyId, typeId, from, to));
    }
}
