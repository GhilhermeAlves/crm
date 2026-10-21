package com.becommerce.crm.sales.holiday.web;

import com.becommerce.crm.sales.holiday.application.dto.CreateHolidayRequest;
import com.becommerce.crm.sales.holiday.application.dto.HolidayResponse;
import com.becommerce.crm.sales.holiday.application.dto.UpdateHolidayRequest;
import com.becommerce.crm.sales.holiday.application.port.in.HolidayUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/companies/{companyId}/holidays")
public class HolidayController {

    private final HolidayUseCase useCase;

    public HolidayController(HolidayUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<HolidayResponse>> list(
            @CurrentCompanyId("Você só pode acessar os feriados da sua própria empresa.") UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null) {
            return ResponseEntity.ok(useCase.listByRange(companyId, from, to));
        }
        return ResponseEntity.ok(useCase.listByCompany(companyId));
    }

    @GetMapping("/{holidayId}")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<HolidayResponse> getById(
            @CurrentCompanyId("Você só pode acessar os feriados da sua própria empresa.") UUID companyId,
            @PathVariable UUID holidayId) {
        return ResponseEntity.ok(useCase.getById(companyId, holidayId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<HolidayResponse> create(
            @CurrentCompanyId("Você só pode acessar os feriados da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateHolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(companyId, request));
    }

    @PutMapping("/{holidayId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<HolidayResponse> update(
            @CurrentCompanyId("Você só pode acessar os feriados da sua própria empresa.") UUID companyId,
            @PathVariable UUID holidayId,
            @Valid @RequestBody UpdateHolidayRequest request) {
        return ResponseEntity.ok(useCase.update(companyId, holidayId, request));
    }

    @DeleteMapping("/{holidayId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar os feriados da sua própria empresa.") UUID companyId,
            @PathVariable UUID holidayId) {
        useCase.delete(companyId, holidayId);
        return ResponseEntity.noContent().build();
    }
}
