package com.becommerce.crm.masterdata.inventory.web;

import com.becommerce.crm.masterdata.inventory.application.dto.InventoryItemResponse;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.dto.StockMovementResponse;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Controle de estoque dos produtos do catálogo (V095). Isolamento por RLS FORCE + {@code @CurrentCompanyId}. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/inventory")
public class InventoryController {

    private static final String OWN_COMPANY = "Você só pode acessar o estoque da sua própria empresa.";

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<PageResponse<InventoryItemResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(inventoryService.listProducts(companyId, q, category, lowStock, active,
                page, pageSize));
    }

    @GetMapping("/{itemId}/movements")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<PageResponse<StockMovementResponse>> movements(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(inventoryService.listMovements(companyId, itemId, page, pageSize));
    }

    @PostMapping("/{itemId}/movements")
    @PreAuthorize("hasAuthority('inventory:manage')")
    public ResponseEntity<StockMovementResponse> register(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID itemId,
            @Valid @RequestBody RegisterMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.registerMovement(companyId, itemId, request));
    }
}