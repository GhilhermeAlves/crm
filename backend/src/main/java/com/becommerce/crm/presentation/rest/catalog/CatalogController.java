package com.becommerce.crm.presentation.rest.catalog;

import com.becommerce.crm.application.catalog.dto.CatalogItemRequest;
import com.becommerce.crm.application.catalog.dto.CatalogItemResponse;
import com.becommerce.crm.application.catalog.service.CatalogService;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.catalog.CatalogItemType;
import com.becommerce.crm.infrastructure.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Catálogo de produtos/serviços (V078). Isolamento por RLS FORCE + {@code @CurrentCompanyId}. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/catalog")
public class CatalogController {

    private static final String OWN_COMPANY = "Você só pode acessar o catálogo da sua própria empresa.";

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalog:read')")
    public ResponseEntity<PageResponse<CatalogItemResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CatalogItemType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(catalogService.list(companyId, q, type, active, page, pageSize));
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAuthority('catalog:read')")
    public ResponseEntity<CatalogItemResponse> getById(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                       @PathVariable UUID itemId) {
        return ResponseEntity.ok(catalogService.getById(companyId, itemId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalog:manage')")
    public ResponseEntity<CatalogItemResponse> create(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                      @Valid @RequestBody CatalogItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.create(companyId, request));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('catalog:manage')")
    public ResponseEntity<CatalogItemResponse> update(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                      @PathVariable UUID itemId,
                                                      @Valid @RequestBody CatalogItemRequest request) {
        return ResponseEntity.ok(catalogService.update(companyId, itemId, request));
    }

    @PostMapping("/{itemId}/activate")
    @PreAuthorize("hasAuthority('catalog:manage')")
    public ResponseEntity<CatalogItemResponse> activate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                        @PathVariable UUID itemId) {
        return ResponseEntity.ok(catalogService.setActive(companyId, itemId, true));
    }

    @PostMapping("/{itemId}/deactivate")
    @PreAuthorize("hasAuthority('catalog:manage')")
    public ResponseEntity<CatalogItemResponse> deactivate(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                                          @PathVariable UUID itemId) {
        return ResponseEntity.ok(catalogService.setActive(companyId, itemId, false));
    }
}
