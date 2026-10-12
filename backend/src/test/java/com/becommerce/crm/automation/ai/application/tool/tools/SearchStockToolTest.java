package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.tools.SearchStockTool.StockItem;
import com.becommerce.crm.masterdata.inventory.application.dto.InventoryItemResponse;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchStockToolTest {

    private final UUID companyId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, UUID.randomUUID(),
            new AiPermissionContext(List.of("inventory:read")));

    private final InventoryService inventoryService = mock(InventoryService.class);
    private final SearchStockTool searchTool = new SearchStockTool(inventoryService);

    private InventoryItemResponse item(String name, String unit, int minStock, int quantity, String sku) {
        return new InventoryItemResponse(UUID.randomUUID(), name, "EPI", unit, minStock, quantity, sku, true);
    }

    @SuppressWarnings("unchecked")
    private List<StockItem> data(AiToolResult result) {
        assertTrue(result.success(), result.error());
        return (List<StockItem>) result.data();
    }

    @Test
    void searchDefaultsToActiveProductsAndComputesLowStock() {
        InventoryItemResponse low = item("Luva A", "UNIDADE", 5, 3, "SKU-1");
        when(inventoryService.listProducts(eq(companyId), isNull(), isNull(), isNull(), eq(true), eq(0), eq(20)))
                .thenReturn(PageResponse.of(List.of(low), 0, 20, 1));

        List<StockItem> result = data(searchTool.execute(ctx, Map.of()));

        assertEquals(1, result.size());
        StockItem summary = result.get(0);
        assertEquals("Luva A", summary.name());
        assertEquals("SKU-1", summary.sku());
        assertEquals(3, summary.quantity());
        assertEquals("UNIDADE", summary.unit());
        assertEquals(5, summary.minStock());
        assertTrue(summary.lowStock());
    }

    @Test
    void searchPassesQueryAndLowStockAndClampsLimit() {
        when(inventoryService.listProducts(eq(companyId), eq("luva"), isNull(), eq(true), eq(true), eq(0), eq(50)))
                .thenReturn(PageResponse.of(List.of(), 0, 50, 0));

        AiToolResult result = searchTool.execute(ctx,
                Map.of("query", "luva", "lowStock", true, "limit", 999));

        assertTrue(result.success(), result.error());
        assertTrue(data(result).isEmpty());
    }

    @Test
    void searchRejectsInvalidLowStock() {
        AiToolResult result = searchTool.execute(ctx, Map.of("lowStock", "talvez"));

        assertFalse(result.success());
    }
}
