package com.becommerce.crm.masterdata.inventory.application.port.output;

import com.becommerce.crm.masterdata.inventory.domain.StockMovement;

import java.util.List;
import java.util.UUID;

/** Porta de escrita/leitura de movimentos ({@code inventory_movements}, V095). RLS FORCE isola o tenant. */
public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    PageResult findByCatalogItem(UUID companyId, UUID catalogItemId, int page, int pageSize);

    record PageResult(List<StockMovement> content, long totalElements) {}
}
