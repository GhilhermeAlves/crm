package com.becommerce.crm.masterdata.inventory.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.port.output.StockMovementRepository;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import com.becommerce.crm.masterdata.inventory.domain.exception.InsufficientStockException;
import com.becommerce.crm.masterdata.inventory.domain.exception.InventoryItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();

    private CatalogItemRepository catalogRepository;
    private StockMovementRepository movementRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        catalogRepository = mock(CatalogItemRepository.class);
        movementRepository = mock(StockMovementRepository.class);
        service = new InventoryService(catalogRepository, movementRepository, mock(TenantAuditRecorder.class));
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CatalogItem product(int quantity) {
        return CatalogItem.reconstitute(UUID.randomUUID(), COMPANY, CatalogItemType.PRODUCT, "Luva",
                null, "EPI", null, "BRL", "SKU", true, "CAIXA", 5, quantity,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    @Test
    void entradaIncreasesQuantityAndStoresPositiveDelta() {
        CatalogItem item = product(2);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        var response = service.registerMovement(COMPANY, item.getId(),
                new RegisterMovementRequest(StockMovementType.ENTRADA, 10, "Compra"));

        assertEquals(12, response.balanceAfter());
        verify(movementRepository).save(any());
    }

    @Test
    void saidaDecreasesQuantity() {
        CatalogItem item = product(10);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        var response = service.registerMovement(COMPANY, item.getId(),
                new RegisterMovementRequest(StockMovementType.SAIDA, 3, "Uso"));

        assertEquals(7, response.balanceAfter());
    }

    @Test
    void ajusteComputesDeltaFromRealCount() {
        CatalogItem item = product(10);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        var response = service.registerMovement(COMPANY, item.getId(),
                new RegisterMovementRequest(StockMovementType.AJUSTE, 6, "Contagem"));

        assertEquals(6, response.balanceAfter());
    }

    @Test
    void saidaBelowZeroIsRejectedAndNothingPersists() {
        CatalogItem item = product(2);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        assertThrows(InsufficientStockException.class, () -> service.registerMovement(COMPANY,
                item.getId(), new RegisterMovementRequest(StockMovementType.SAIDA, 5, null)));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void serviceItemIsNotAStockItem() {
        CatalogItem serviceItem = CatalogItem.reconstitute(UUID.randomUUID(), COMPANY,
                CatalogItemType.SERVICE, "Consulta", null, null, null, "BRL", null, true, null, 0, 0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(catalogRepository.findByIdForUpdate(serviceItem.getId())).thenReturn(Optional.of(serviceItem));

        assertThrows(InventoryItemNotFoundException.class, () -> service.registerMovement(COMPANY,
                serviceItem.getId(), new RegisterMovementRequest(StockMovementType.ENTRADA, 1, null)));
    }

    @Test
    void itemOfAnotherCompanyIsNotFound() {
        CatalogItem other = product(1);
        when(catalogRepository.findByIdForUpdate(other.getId())).thenReturn(Optional.of(other));

        assertThrows(InventoryItemNotFoundException.class, () -> service.registerMovement(
                UUID.randomUUID(), other.getId(), new RegisterMovementRequest(
                        StockMovementType.ENTRADA, 1, null)));
    }
}
