package com.becommerce.crm.presentation.rest.company;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CompanySummaryResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.application.company.dto.UpdateCompanyRequest;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class CompanyController {

    private final CompanyUseCase companyUseCase;

    public CompanyController(CompanyUseCase companyUseCase) {
        this.companyUseCase = companyUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('company:view')")
    public ResponseEntity<List<CompanySummaryResponse>> list() {
        return ResponseEntity.ok(companyUseCase.listCompanies());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('company:view')")
    public ResponseEntity<CompanyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(companyUseCase.getCompanyById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('company:create')")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse response = companyUseCase.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<CompanyResponse> update(@PathVariable UUID id,
                                                  @RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(companyUseCase.updateCompany(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('company:delete')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        companyUseCase.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}
