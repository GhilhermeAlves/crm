package com.becommerce.crm.sales.scheduling.web;

import com.becommerce.crm.sales.scheduling.application.dto.AgendaSettingsDto;
import com.becommerce.crm.sales.scheduling.application.port.in.AgendaSettingsUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AgendaSettingsController {

    private final AgendaSettingsUseCase useCase;

    public AgendaSettingsController(AgendaSettingsUseCase useCase) {
        this.useCase = useCase;
    }

    /** Leitura liberada para quem vê a agenda: ela usa a duração padrão e o horário de trabalho. */
    @GetMapping("/api/v1/companies/{companyId}/agenda-settings")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<AgendaSettingsDto> get(
            @CurrentCompanyId("Você só pode acessar configurações da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(useCase.get(companyId));
    }

    @PutMapping("/api/v1/companies/{companyId}/agenda-settings")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<AgendaSettingsDto> save(
            @CurrentCompanyId("Você só pode acessar configurações da sua própria empresa.") UUID companyId,
            @Valid @RequestBody AgendaSettingsDto request) {
        return ResponseEntity.ok(useCase.save(companyId, request));
    }
}
