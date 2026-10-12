package com.becommerce.crm.masterdata.catalog.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemRequest;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogItemNotFoundException;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogSkuConflictException;
import com.becommerce.crm.masterdata.catalog.domain.exception.InvalidCatalogItemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();

    private CatalogItemRepository repository;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        repository = mock(CatalogItemRepository.class);
        service = new CatalogService(repository, mock(TenantAuditRecorder.class));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CatalogItemRequest request(String sku) {
        return new CatalogItemRequest(CatalogItemType.PRODUCT, "Plano Pro", null, null,
                new BigDecimal("10.00"), "BRL", sku, null, null);
    }

    @Test
    void createRejectsDuplicateSku() {
        when(repository.existsBySku(COMPANY, "SKU-1", null)).thenReturn(true);

        assertThrows(CatalogSkuConflictException.class, () -> service.create(COMPANY, request("SKU-1")));
        verify(repository, never()).save(any());
    }

    @Test
    void createWithoutSkuSkipsUniquenessCheck() {
        service.create(COMPANY, request(" "));
        verify(repository, never()).existsBySku(any(), any(), any());
    }

    @Test
    void createRejectsInvalidProductCategory() {
        assertThrows(InvalidCatalogItemException.class, () -> service.create(COMPANY,
                new CatalogItemRequest(CatalogItemType.PRODUCT, "X", null, "INVALID",
                        null, "BRL", null, null, null)));
    }

    @Test
    void serviceItemNeverHasStockFields() {
        service.create(COMPANY, new CatalogItemRequest(CatalogItemType.SERVICE, "Consulta", null,
                "Consultas", null, "BRL", null, "CAIXA", 7));
        verify(repository).save(argThat(i ->
                i.getType() == CatalogItemType.SERVICE && i.getUnit() == null && i.getMinStock() == 0));
    }

    @Test
    void updateIgnoresOwnSkuButRejectsAnothersSku() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "A", null, null, null, null,
                "SKU-1", null, 0);
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));
        when(repository.existsBySku(COMPANY, "SKU-2", item.getId())).thenReturn(true);

        assertThrows(CatalogSkuConflictException.class,
                () -> service.update(COMPANY, item.getId(), request("SKU-2")));
    }

    @Test
    void itemOfAnotherCompanyIsNotFound() {
        CatalogItem other = CatalogItem.create(UUID.randomUUID(), CatalogItemType.PRODUCT, "A", null, null,
                null, null, null, null, 0);
        when(repository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThrows(CatalogItemNotFoundException.class, () -> service.getById(COMPANY, other.getId()));
        assertThrows(CatalogItemNotFoundException.class,
                () -> service.setActive(COMPANY, other.getId(), false));
    }

    @Test
    void deactivatePersistsInactiveItem() {
        CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.SERVICE, "A", null, null, null, null,
                null, null, 0);
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));

        assertFalse(service.setActive(COMPANY, item.getId(), false).active());
    }

    @Test
    void listClampsPageSizeAndBlankQuery() {
        when(repository.search(eq(COMPANY), isNull(), isNull(), isNull(), eq(0), eq(100)))
                .thenReturn(new CatalogItemRepository.PageResult(List.of(), 0));

        assertEquals(100, service.list(COMPANY, "  ", null, null, -3, 5000).pageSize());
    }
}
