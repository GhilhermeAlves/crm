package com.becommerce.crm.sales.chair.web;

import com.becommerce.crm.sales.chair.application.dto.ChairResponse;
import com.becommerce.crm.sales.chair.application.dto.CreateChairRequest;
import com.becommerce.crm.sales.chair.application.dto.UpdateChairRequest;
import com.becommerce.crm.sales.chair.application.port.in.ChairUseCase;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/chairs")
public class ChairController {

    private final ChairUseCase useCase;

    public ChairController(ChairUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<List<ChairResponse>> list(
            @CurrentCompanyId("Você só pode acessar as cadeiras da sua própria empresa.") UUID companyId) {
        return ResponseEntity.ok(useCase.listByCompany(companyId));
    }

    @GetMapping("/{chairId}")
    @PreAuthorize("hasAuthority('appointment:read')")
    public ResponseEntity<ChairResponse> getById(
            @CurrentCompanyId("Você só pode acessar as cadeiras da sua própria empresa.") UUID companyId,
            @PathVariable UUID chairId) {
        return ResponseEntity.ok(useCase.getById(companyId, chairId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<ChairResponse> create(
            @CurrentCompanyId("Você só pode acessar as cadeiras da sua própria empresa.") UUID companyId,
            @Valid @RequestBody CreateChairRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(companyId, request));
    }

    @PutMapping("/{chairId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<ChairResponse> update(
            @CurrentCompanyId("Você só pode acessar as cadeiras da sua própria empresa.") UUID companyId,
            @PathVariable UUID chairId,
            @Valid @RequestBody UpdateChairRequest request) {
        return ResponseEntity.ok(useCase.update(companyId, chairId, request));
    }

    @DeleteMapping("/{chairId}")
    @PreAuthorize("hasAuthority('scheduling:configure')")
    public ResponseEntity<Void> delete(
            @CurrentCompanyId("Você só pode acessar as cadeiras da sua própria empresa.") UUID companyId,
            @PathVariable UUID chairId) {
        useCase.delete(companyId, chairId);
        return ResponseEntity.noContent().build();
    }
}
