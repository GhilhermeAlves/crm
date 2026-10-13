package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.masterdata.inventory.application.dto.InventoryItemResponse;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code search_stock}: lista os produtos do estoque (saldo, unidade e estoque
 * mínimo), com filtros opcionais de nome/SKU e de baixo estoque. Fornece o
 * {@code itemId} usado por {@code register_stock_movement}. Exige
 * {@code inventory:read}.
 */
@Component
public class SearchStockTool extends AbstractAiReadTool {

    private static final String NAME = "search_stock";

    private final InventoryService inventoryService;

    @Autowired
    public SearchStockTool(InventoryService inventoryService) {
        super(NAME, "Busca os produtos do estoque com saldo, unidade e estoque mínimo. Filtra por "
                        + "nome/SKU e por itens com baixo estoque. Use antes de registrar uma movimentação "
                        + "para obter o itemId e o saldo atual.",
                "inventory:read",
                Map.of(
                        "query", stringProp("Nome ou SKU do produto"),
                        "lowStock", boolProp("Se true, retorna só itens com baixo estoque"),
                        "limit", integerProp("Máximo de resultados (1 a 50, padrão 20)")));
        this.inventoryService = inventoryService;
    }

    public record StockItem(UUID id, String name, String sku, int quantity, String unit, int minStock,
                            boolean lowStock) {

        static StockItem from(InventoryItemResponse item) {
            return new StockItem(item.id(), item.name(), item.sku(), item.quantity(), item.unit(),
                    item.minStock(), item.minStock() > 0 && item.quantity() <= item.minStock());
        }
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        String query = string(arguments, "query");
        Boolean lowStock = bool(arguments, "lowStock");
        int limit = clampLimit(integer(arguments, "limit"));

        List<StockItem> items = inventoryService
                .listProducts(ctx.companyId(), query, null, lowStock, true, 0, limit)
                .content().stream()
                .map(StockItem::from)
                .toList();
        return AiToolResult.ok(NAME, items);
    }

    private static Boolean bool(Map<String, Object> args, String key) {
        Object value = args == null ? null : args.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(s)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(s)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Valor inválido para '" + key + "': " + value);
    }

    private static Map<String, Object> boolProp(String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "boolean");
        p.put("description", description);
        return p;
    }
}
