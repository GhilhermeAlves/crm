package com.becommerce.crm.presentation.rest.me;

import com.becommerce.crm.application.me.dto.CompanyOptionResponse;
import com.becommerce.crm.application.me.dto.SwitchCompanyRequest;
import com.becommerce.crm.application.me.port.input.MeUseCase;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Company Switcher (Sprint 8.4).
 *
 * <pre>
 *   GET  /api/v1/me/companies      → empresas do usuário com membership ativa
 *   POST /api/v1/me/switch-company → alterna a empresa ativa corrente
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final MeUseCase meUseCase;

    public MeController(MeUseCase meUseCase) {
        this.meUseCase = meUseCase;
    }

    @GetMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompanyOptionResponse>> myCompanies(
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(meUseCase.listMyCompanies(principal.userId()));
    }

    @PostMapping("/switch-company")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyOptionResponse> switchCompany(
            @Valid @RequestBody SwitchCompanyRequest request,
            @AuthenticationPrincipal CurrentUser principal) {
        return ResponseEntity.ok(meUseCase.switchCompany(principal.userId(), request.companyId()));
    }
}