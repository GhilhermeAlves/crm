package com.becommerce.crm.masterdata.company.web;

import com.becommerce.crm.masterdata.company.application.dto.CompanyModulesResponse;
import com.becommerce.crm.masterdata.company.application.dto.UpdateCompanyModulesRequest;
import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Segmento e módulos habilitados da empresa (V104). Alteração exclusiva do SUPER_ADMIN. */
@RestController
@RequestMapping({"/api/v1/companies/{id}/modules", "/api/v1/tenants/{id}/modules"})
public class CompanyModulesController {

    private final CompanyModulesService service;

    public CompanyModulesController(CompanyModulesService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyModulesResponse> get(@PathVariable UUID id,
                                                      @AuthenticationPrincipal CurrentUser principal) {
        boolean superAdmin = principal.roles().contains("SUPER_ADMIN");
        return ResponseEntity.ok(service.get(id, principal.companyId(), superAdmin));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyModulesResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateCompanyModulesRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
}
