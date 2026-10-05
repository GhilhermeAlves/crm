package com.becommerce.crm.sales.scheduling.web;

import com.becommerce.crm.sales.scheduling.application.dto.BirthdayMessageSettingsDto;
import com.becommerce.crm.sales.scheduling.application.port.in.BirthdayMessageSettingsUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class BirthdayMessageSettingsController {

    private final BirthdayMessageSettingsUseCase useCase;

    public BirthdayMessageSettingsController(BirthdayMessageSettingsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/api/v1/companies/{companyId}/birthday-message")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<BirthdayMessageSettingsDto> get(
            @CurrentCompanyId("Você só pode acessar configurações da sua própria empresa.") UUID companyId) {
        return useCase.get(companyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/api/v1/companies/{companyId}/birthday-message")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<BirthdayMessageSettingsDto> save(
            @CurrentCompanyId("Você só pode acessar configurações da sua própria empresa.") UUID companyId,
            @Valid @RequestBody BirthdayMessageSettingsDto request) {
        return ResponseEntity.ok(useCase.save(companyId, request));
    }
}
