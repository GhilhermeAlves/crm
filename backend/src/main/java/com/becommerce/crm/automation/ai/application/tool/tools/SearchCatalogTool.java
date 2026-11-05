package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemResponse;
import com.becommerce.crm.masterdata.catalog.application.service.CatalogService;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code search_catalog}: servicos ativos do catalogo (procedimentos e exames)
 * com id, nome, categoria e preco, para {@code create_procedure}. Exige {@code catalog:read}.
 */
@Component
public class SearchCatalogTool extends AbstractAiReadTool {

    private static final String NAME = "search_catalog";
    private static final int LIMIT = 20;

    private final CatalogService catalogService;

    public SearchCatalogTool(CatalogService catalogService) {
        super(NAME, "Busca procedimentos e exames (servicos ativos do catalogo) por nome. Devolve id, nome, "
                        + "categoria e preco (catalogItemId de create_procedure).",
                "catalog:read",
                Map.of("query", Map.of("type", "string", "description", "Parte do nome do procedimento/exame")));
        this.catalogService = catalogService;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        Object raw = arguments.get("query");
        String query = raw == null ? null : String.valueOf(raw).trim();
        var items = catalogService.list(ctx.companyId(), query, CatalogItemType.SERVICE, true, 0, LIMIT)
                .content().stream()
                .map(SearchCatalogTool::toMap)
                .toList();
        return AiToolResult.ok(NAME, items);
    }

    private static Map<String, Object> toMap(CatalogItemResponse i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.id());
        m.put("name", i.name());
        m.put("category", i.category());
        m.put("price", i.price());
        return m;
    }
}
