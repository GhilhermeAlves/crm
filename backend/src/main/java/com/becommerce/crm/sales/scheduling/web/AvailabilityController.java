package com.becommerce.crm.sales.scheduling.web;

import com.becommerce.crm.sales.scheduling.application.dto.AvailabilityResponse;
import com.becommerce.crm.sales.scheduling.application.dto.SetAvailabilityRequest;
import com.becommerce.crm.sales.scheduling.application.port.in.AvailabilityUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AvailabilityController {

    private final AvailabilityUseCase useCase;

    public AvailabilityController(AvailabilityUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/users/{userId}/availability")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<AvailabilityResponse> get(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(useCase.get(companyId, userId));
    }

    @PutMapping("/api/v1/companies/{companyId}/users/{userId}/availability")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<AvailabilityResponse> set(
            @CurrentCompanyId("Você só pode acessar agendamentos da sua própria empresa.") UUID companyId,
            @PathVariable UUID userId,
            @RequestBody SetAvailabilityRequest request) {
        return ResponseEntity.ok(useCase.set(companyId, userId, request));
    }
}
