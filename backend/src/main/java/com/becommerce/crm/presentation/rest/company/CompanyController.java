package com.becommerce.crm.presentation.rest.company;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CompanySettingsResponse;
import com.becommerce.crm.application.company.dto.CompanySummaryResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.application.company.dto.UpdateCompanyRequest;
import com.becommerce.crm.application.company.dto.UpdateCompanySettingsRequest;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/companies", "/api/v1/tenants"})
public class CompanyController {

    private final CompanyUseCase companyUseCase;

    public CompanyController(CompanyUseCase companyUseCase) {
        this.companyUseCase = companyUseCase;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompanySummaryResponse>> list(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(companyUseCase.listCompanies(principal.companyId(), isSuperAdmin(principal)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponse> getMe(@AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(companyUseCase.getCompanyById(
                principal.companyId(), principal.companyId(), isSuperAdmin(principal)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponse> getById(@PathVariable UUID id,
                                                   @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(companyUseCase.getCompanyById(id, principal.companyId(), isSuperAdmin(principal)));
    }

    @GetMapping("/{id}/settings")
    @PreAuthorize("hasAuthority('settings:view')")
    public ResponseEntity<CompanySettingsResponse> getSettings(@PathVariable UUID id,
                                                               @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(companyUseCase.getCompanySettings(id, principal.companyId()));
    }

    @PutMapping("/{id}/settings")
    @PreAuthorize("hasAuthority('settings:update')")
    public ResponseEntity<CompanySettingsResponse> updateSettings(@PathVariable UUID id,
                                                                  @RequestBody UpdateCompanySettingsRequest request,
                                                                  @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(companyUseCase.updateCompanySettings(id, request, principal.companyId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('company:create')")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request,
                                                  @AuthenticationPrincipal CurrentUser principal) {
        CompanyResponse response = companyUseCase.createCompany(request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<CompanyResponse> update(@PathVariable UUID id,
                                                  @RequestBody UpdateCompanyRequest request,
                                                  @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(companyUseCase.updateCompany(id, request, principal.companyId(), isSuperAdmin(principal)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('company:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal CurrentUser principal) {
        companyUseCase.deleteCompany(id, principal.companyId(), isSuperAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    private boolean isSuperAdmin(CurrentUser principal) {
        return principal.roles().contains("SUPER_ADMIN");
    }
}
