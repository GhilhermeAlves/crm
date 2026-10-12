package com.becommerce.crm.masterdata.inventory.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.inventory.application.dto.InventoryItemResponse;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.dto.StockMovementResponse;
import com.becommerce.crm.masterdata.inventory.application.port.output.StockMovementRepository;
import com.becommerce.crm.masterdata.inventory.domain.StockMovement;
import com.becommerce.crm.masterdata.inventory.domain.exception.InsufficientStockException;
import com.becommerce.crm.masterdata.inventory.domain.exception.InventoryItemNotFoundException;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CatalogItemRepository catalogRepository;
    private final StockMovementRepository movementRepository;
    private final TenantAuditRecorder auditor;

    public InventoryService(CatalogItemRepository catalogRepository,
                            StockMovementRepository movementRepository, TenantAuditRecorder auditor) {
        this.catalogRepository = catalogRepository;
        this.movementRepository = movementRepository;
        this.auditor = auditor;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> listProducts(UUID companyId, String query, String category,
                                                            Boolean lowStock, Boolean active,
                                                            int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        try {
            TenantContext.setCompanyId(companyId);
            var result = catalogRepository.searchProducts(companyId, blankToNull(query), blankToNull(category),
                    lowStock, active, safePage, size);
            return PageResponse.of(result.content().stream().map(InventoryItemResponse::from).toList(),
                    safePage, size, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> listMovements(UUID companyId, UUID itemId, int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        try {
            TenantContext.setCompanyId(companyId);
            var result = movementRepository.findByCatalogItem(companyId, itemId, safePage, size);
            return PageResponse.of(result.content().stream().map(m -> StockMovementResponse.from(m, null)).toList(),
                    safePage, size, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public StockMovementResponse registerMovement(UUID companyId, UUID itemId, RegisterMovementRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            CatalogItem item = requireProduct(companyId, itemId);
            int delta = switch (request.type()) {
                case ENTRADA -> request.quantity();
                case SAIDA -> -request.quantity();
                case AJUSTE -> request.quantity() - item.getQuantity();
            };
            int newQuantity = item.getQuantity() + delta;
            if (newQuantity < 0) {
                throw new InsufficientStockException(item.getName(), item.getQuantity(), request.quantity());
            }
            item.updateQuantity(newQuantity);
            CatalogItem saved = catalogRepository.save(item);
            StockMovement movement = movementRepository.save(StockMovement.create(
                    companyId, item.getId(), request.type(), delta, request.note()));
            auditor.record(companyId, AuditAction.CREATE, AuditModule.INVENTORY, "InventoryMovement",
                    movement.getId().toString(),
                    "Movimento de estoque: " + request.type() + " (" + delta + ") em " + saved.getName(),
                    null, Map.of("type", request.type().name(), "delta", String.valueOf(delta)));
            return StockMovementResponse.from(movement, newQuantity);
        } finally {
            TenantContext.clear();
        }
    }

    private CatalogItem requireProduct(UUID companyId, UUID itemId) {
        CatalogItem item = catalogRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new InventoryItemNotFoundException(itemId));
        if (!item.getCompanyId().equals(companyId) || item.getType() != CatalogItemType.PRODUCT) {
            throw new InventoryItemNotFoundException(itemId);
        }
        return item;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
