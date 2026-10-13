package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemResponse;
import com.becommerce.crm.masterdata.catalog.application.service.CatalogService;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code register_stock_movement}: valida o item (existe, é da
 * empresa, é produto ativo) e a quantidade, rejeitando saída maior que o saldo
 * atual, e PERSISTE uma proposta de movimentação de estoque
 * ({@code AiActionService#propose}). A movimentação só ocorre após confirmação
 * explícita do usuário. Requer {@code inventory:manage}.
 */
@Component
public class RegisterStockMovementTool implements AiTool {

    private static final String NAME = "register_stock_movement";
    private static final int MAX_NOTE = 500;

    private final AiActionService actionService;
    private final CatalogService catalogService;

    @Autowired
    public RegisterStockMovementTool(@Lazy AiActionService actionService, CatalogService catalogService) {
        this.actionService = actionService;
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe registrar ENTRADA, SAIDA ou AJUSTE de estoque de um produto. Nao executa: cria "
                + "uma proposta que o usuario confirma. Antes, use search_stock para obter o itemId e o "
                + "saldo atual.";
    }

    @Override
    public String requiredPermission() {
        return "inventory:manage";
    }

    @Override
    public String risk() {
        return "WRITE";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "itemId", Map.of("type", "string", "format", "uuid",
                        "description", "ID do produto (obtido em search_stock)"),
                "type", Map.of("type", "string", "enum", List.of("ENTRADA", "SAIDA", "AJUSTE"),
                        "description", "ENTRADA (compra/adição), SAIDA (uso/perda) ou AJUSTE (contagem real)"),
                "quantity", Map.of("type", "integer", "minimum", 1,
                        "description", "Quantidade (mínimo 1). Em AJUSTE é a contagem real."),
                "note", Map.of("type", "string", "description", "Observação (máx 500 caracteres)")));
        schema.put("required", List.of("itemId", "type", "quantity"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        UUID itemId = uuid(args, "itemId");
        if (itemId == null) {
            throw new IllegalArgumentException("Informe o itemId do produto.");
        }
        StockMovementType type = requiredType(args);
        int quantity = requiredQuantity(args);
        String note = text(args, "note");
        if (note != null && note.length() > MAX_NOTE) {
            throw new IllegalArgumentException("A observação deve ter no máximo 500 caracteres.");
        }
        CatalogItemResponse item = catalogService.getById(ctx.companyId(), itemId);
        if (item.type() != CatalogItemType.PRODUCT) {
            throw new IllegalArgumentException("O item não é um produto de estoque.");
        }
        if (!item.active()) {
            throw new IllegalArgumentException("O produto está inativo e não pode movimentar estoque.");
        }
        int current = item.quantity();
        if (type == StockMovementType.SAIDA && quantity > current) {
            throw new IllegalArgumentException("Saldo insuficiente: há " + current + " " + unitOf(item) + ".");
        }

        Map<String, Object> parameters = newMap();
        put(parameters, "itemId", itemId);
        put(parameters, "type", type.name());
        put(parameters, "quantity", quantity);
        put(parameters, "note", note);

        String description = buildDescription(type, quantity, item.name(), current);
        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "INVENTORY_ITEM", itemId, parameters, description);
        return AiToolResult.ok(NAME, proposal);
    }

    private StockMovementType requiredType(Map<String, Object> args) {
        String raw = text(args, "type");
        if (raw == null) {
            throw new IllegalArgumentException("Informe o tipo: ENTRADA, SAIDA ou AJUSTE.");
        }
        try {
            return StockMovementType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo inválido: use ENTRADA, SAIDA ou AJUSTE.");
        }
    }

    private int requiredQuantity(Map<String, Object> args) {
        Object value = args.get("quantity");
        if (value == null) {
            throw new IllegalArgumentException("Informe a quantidade.");
        }
        int quantity;
        try {
            quantity = new BigDecimal(String.valueOf(value)).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Quantidade inválida.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
        return quantity;
    }

    private static String unitOf(CatalogItemResponse item) {
        return item.unit() == null || item.unit().isBlank() ? "un" : item.unit();
    }

    private static String buildDescription(StockMovementType type, int quantity, String name, int current) {
        return switch (type) {
            case ENTRADA -> "Entrada de estoque: " + quantity + " un de " + name
                    + " (saldo " + current + " → " + (current + quantity) + ")";
            case SAIDA -> "Saída de estoque: " + quantity + " un de " + name
                    + " (saldo " + current + " → " + (current - quantity) + ")";
            case AJUSTE -> "Ajuste de estoque: " + name + " para " + quantity + " un";
        };
    }
}
