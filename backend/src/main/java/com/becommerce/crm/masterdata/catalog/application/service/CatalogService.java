package com.becommerce.crm.masterdata.catalog.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemRequest;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemResponse;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogItemNotFoundException;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogSkuConflictException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Catálogo de produtos/serviços (V078). Isolamento por {@code TenantContext} + RLS FORCE.
 * Usado pela página /catalog e, em modo leitura de itens ativos, pelo agente de IA.
 */
@Service
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CatalogItemRepository repository;
    private final TenantAuditRecorder auditor;

    public CatalogService(CatalogItemRepository repository, TenantAuditRecorder auditor) {
        this.repository = repository;
        this.auditor = auditor;
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogItemResponse> list(UUID companyId, String query, CatalogItemType type,
                                                  Boolean active, int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        try {
            TenantContext.setCompanyId(companyId);
            var result = repository.search(companyId, blankToNull(query), type, active, safePage, size);
            return PageResponse.of(result.content().stream().map(CatalogItemResponse::from).toList(),
                    safePage, size, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse getById(UUID companyId, UUID itemId) {
        try {
            TenantContext.setCompanyId(companyId);
            return CatalogItemResponse.from(requireOwned(companyId, itemId));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public CatalogItemResponse create(UUID companyId, CatalogItemRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            ensureSkuFree(companyId, request.sku(), null);
            CatalogItem saved = repository.save(CatalogItem.create(companyId, request.type(), request.name(),
                    request.description(), request.category(), request.price(), request.currency(),
                    request.sku()));
            auditor.record(companyId, AuditAction.CREATE, AuditModule.CATALOG, "CatalogItem",
                    saved.getId().toString(), "Item de catálogo criado: " + saved.getName(), null,
                    Map.of("type", saved.getType().name()));
            return CatalogItemResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public CatalogItemResponse update(UUID companyId, UUID itemId, CatalogItemRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            CatalogItem item = requireOwned(companyId, itemId);
            ensureSkuFree(companyId, request.sku(), itemId);
            item.update(request.type(), request.name(), request.description(), request.category(),
                    request.price(), request.currency(), request.sku());
            CatalogItem saved = repository.save(item);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CATALOG, "CatalogItem",
                    saved.getId().toString(), "Item de catálogo atualizado: " + saved.getName(), null,
                    Map.of("type", saved.getType().name()));
            return CatalogItemResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public CatalogItemResponse setActive(UUID companyId, UUID itemId, boolean active) {
        try {
            TenantContext.setCompanyId(companyId);
            CatalogItem item = requireOwned(companyId, itemId);
            if (active) {
                item.activate();
            } else {
                item.deactivate();
            }
            CatalogItem saved = repository.save(item);
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CATALOG, "CatalogItem",
                    saved.getId().toString(),
                    (active ? "Item de catálogo ativado: " : "Item de catálogo desativado: ") + saved.getName(),
                    null, Map.of("active", String.valueOf(active)));
            return CatalogItemResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    private CatalogItem requireOwned(UUID companyId, UUID itemId) {
        CatalogItem item = repository.findById(itemId).orElseThrow(() -> new CatalogItemNotFoundException(itemId));
        if (!item.getCompanyId().equals(companyId)) {
            throw new CatalogItemNotFoundException(itemId);
        }
        return item;
    }

    private void ensureSkuFree(UUID companyId, String sku, UUID excludeId) {
        String normalized = blankToNull(sku);
        if (normalized != null && repository.existsBySku(companyId, normalized, excludeId)) {
            throw new CatalogSkuConflictException(normalized);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
