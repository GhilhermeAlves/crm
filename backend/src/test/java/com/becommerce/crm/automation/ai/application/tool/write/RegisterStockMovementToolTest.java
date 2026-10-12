package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionExecutor;
import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemResponse;
import com.becommerce.crm.masterdata.catalog.application.service.CatalogService;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterStockMovementToolTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("inventory:manage")), conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final CatalogService catalogService = mock(CatalogService.class);

    private final RegisterStockMovementTool movementTool =
            new RegisterStockMovementTool(actionService, catalogService);

    @BeforeEach
    void setUp() {
        when(actionService.propose(any(), any(), any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(new AiActionResponse(UUID.randomUUID(), conversationId, "register_stock_movement",
                        "INVENTORY_ITEM", null, "Proposta", "PROPOSED", Map.of(), null, null,
                        LocalDateTime.now(), LocalDateTime.now()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> proposedParams() {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId),
                eq("register_stock_movement"), eq("INVENTORY_ITEM"), any(), params.capture(), anyString());
        return params.getValue();
    }

    private CatalogItemResponse product(UUID id, int quantity, boolean active) {
        return new CatalogItemResponse(id, companyId, CatalogItemType.PRODUCT, "Luva A", null, "EPI",
                null, "BRL", "SKU-1", null, 5, quantity, active, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void entradaProposesWithoutMovingStock() {
        UUID id = UUID.randomUUID();
        when(catalogService.getById(companyId, id)).thenReturn(product(id, 5, true));

        var result = movementTool.execute(ctx, Map.of(
                "itemId", id.toString(), "type", "ENTRADA", "quantity", 10, "note", "Compra"));

        assertTrue(result.success(), result.error());
        Map<String, Object> params = proposedParams();
        assertEquals(id, params.get("itemId"));
        assertEquals("ENTRADA", params.get("type"));
        assertEquals(10, params.get("quantity"));
        assertEquals("Compra", params.get("note"));
        verify(actionService).propose(companyId, userId, conversationId, "register_stock_movement",
                "INVENTORY_ITEM", id, params,
                "Entrada de estoque: 10 un de Luva A (saldo 5 → 15)");
    }

    @Test
    void saidaProposesWithNewBalance() {
        UUID id = UUID.randomUUID();
        when(catalogService.getById(companyId, id)).thenReturn(product(id, 10, true));

        movementTool.execute(ctx, Map.of("itemId", id.toString(), "type", "SAIDA", "quantity", 3));

        Map<String, Object> params = proposedParams();
        assertEquals("SAIDA", params.get("type"));
        assertEquals(3, params.get("quantity"));
        verify(actionService).propose(companyId, userId, conversationId, "register_stock_movement",
                "INVENTORY_ITEM", id, params,
                "Saída de estoque: 3 un de Luva A (saldo 10 → 7)");
    }

    @Test
    void ajusteProposesRealCount() {
        UUID id = UUID.randomUUID();
        when(catalogService.getById(companyId, id)).thenReturn(product(id, 10, true));

        movementTool.execute(ctx, Map.of("itemId", id.toString(), "type", "AJUSTE", "quantity", 12));

        Map<String, Object> params = proposedParams();
        assertEquals("AJUSTE", params.get("type"));
        verify(actionService).propose(companyId, userId, conversationId, "register_stock_movement",
                "INVENTORY_ITEM", id, params, "Ajuste de estoque: Luva A para 12 un");
    }

    @Test
    void saidaAboveBalanceIsRejected() {
        UUID id = UUID.randomUUID();
        when(catalogService.getById(companyId, id)).thenReturn(product(id, 2, true));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                movementTool.execute(ctx, Map.of("itemId", id.toString(), "type", "SAIDA", "quantity", 5)));
        assertEquals("Saldo insuficiente: há 2 un.", error.getMessage());
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void rejectsInactiveProduct() {
        UUID id = UUID.randomUUID();
        when(catalogService.getById(companyId, id)).thenReturn(product(id, 5, false));

        assertThrows(IllegalArgumentException.class, () ->
                movementTool.execute(ctx, Map.of("itemId", id.toString(), "type", "ENTRADA", "quantity", 1)));
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void rejectsNonProductItem() {
        UUID id = UUID.randomUUID();
        when(catalogService.getById(companyId, id)).thenReturn(new CatalogItemResponse(id, companyId,
                CatalogItemType.SERVICE, "Consulta", null, null, null, "BRL", null, null, 0, 0, true,
                LocalDateTime.now(), LocalDateTime.now()));

        assertThrows(IllegalArgumentException.class, () ->
                movementTool.execute(ctx, Map.of("itemId", id.toString(), "type", "ENTRADA", "quantity", 1)));
    }

    @Test
    void rejectsMissingItemIdAndInvalidQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                movementTool.execute(ctx, Map.of("type", "ENTRADA", "quantity", 1)));
        assertThrows(IllegalArgumentException.class, () ->
                movementTool.execute(ctx, Map.of("itemId", UUID.randomUUID().toString(),
                        "type", "ENTRADA", "quantity", 0)));
    }

    @Test
    void executorRegistersMovementWithPersistedValues() {
        InventoryService inventoryService = mock(InventoryService.class);
        AiActionExecutor executor = new AiActionExecutor(mock(TaskUseCase.class), mock(ActivityUseCase.class),
                mock(OpportunityUseCase.class), mock(AppointmentUseCase.class),
                mock(FinancialTransactionService.class), inventoryService);
        UUID id = UUID.randomUUID();

        AiAction action = mock(AiAction.class);
        when(action.getTool()).thenReturn("register_stock_movement");
        when(action.getCompanyId()).thenReturn(companyId);
        when(action.getUserId()).thenReturn(userId);
        when(action.getParameters()).thenReturn(Map.of("itemId", id.toString(), "type", "ENTRADA",
                "quantity", 10, "note", "Compra"));

        executor.execute(action);

        verify(inventoryService).registerMovement(companyId, id,
                new RegisterMovementRequest(StockMovementType.ENTRADA, 10, "Compra"));
    }
}
