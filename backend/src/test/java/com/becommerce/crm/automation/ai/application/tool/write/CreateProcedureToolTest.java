package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionExecutor;
import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.tools.SearchCatalogTool;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.port.in.PatientProcedureUseCase;
import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemResponse;
import com.becommerce.crm.masterdata.catalog.application.service.CatalogService;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.catalog.domain.exception.CatalogItemNotFoundException;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import com.becommerce.crm.shared.application.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateProcedureToolTest {

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID facetaId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("contact:update")), conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final ContactUseCase contacts = mock(ContactUseCase.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final CreateProcedureTool tool = new CreateProcedureTool(actionService, contacts, catalog);

    @BeforeEach
    void setUp() {
        ContactResponse contact = mock(ContactResponse.class);
        when(contact.companyId()).thenReturn(companyId);
        when(contact.firstName()).thenReturn("Lúcia");
        when(contact.lastName()).thenReturn("Silva");
        when(contacts.getById(companyId, contactId)).thenReturn(contact);
        when(catalog.getById(companyId, facetaId))
                .thenReturn(item(facetaId, "Faceta em resina composta", "Estética", "750.00"));
        when(catalog.getById(companyId, examId))
                .thenReturn(item(examId, "Radiografia panorâmica", "Exames de Imagem", "120.00"));
    }

    private CatalogItemResponse item(UUID id, String name, String category, String price) {
        return new CatalogItemResponse(id, companyId, CatalogItemType.SERVICE, name, null, category,
                new BigDecimal(price), "BRL", null, null, null, 0, true, null, null);
    }

    @Test
    void permissionIsContactUpdate() {
        assertEquals("contact:update", tool.requiredPermission());
        assertEquals("WRITE", tool.risk());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validMultiToothProposalShowsTotal() {
        tool.execute(ctx, Map.of("contactId", contactId.toString(), "catalogItemId", facetaId.toString(),
                "teeth", List.of(21, 22, 23, 24, 25, 11, 12, 13, 14, 15)));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId), eq("create_procedure"),
                eq("CONTACT"), eq(contactId), params.capture(), description.capture());
        assertEquals("Lançar procedimento para Lúcia Silva: 10 × Faceta em resina composta "
                + "(dentes 11–15, 21–25) — R$ 750,00 cada, total R$ 7.500,00. A realizar. "
                + "Sem lançamento financeiro.", description.getValue());
        assertEquals(75000L, params.getValue().get("priceCents"));
        assertEquals("PERMANENT", params.getValue().get("dentition"));
        assertEquals("PLANNED", params.getValue().get("state"));
        assertEquals(List.of(11, 12, 13, 14, 15, 21, 22, 23, 24, 25), params.getValue().get("teeth"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void examWithoutTeeth() {
        tool.execute(ctx, Map.of("contactId", contactId.toString(), "catalogItemId", examId.toString()));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(actionService).propose(any(), any(), any(), anyString(), anyString(), any(), params.capture(),
                description.capture());
        assertEquals("Lançar exame para Lúcia Silva: 1 × Radiografia panorâmica — R$ 120,00. A realizar. "
                + "Sem lançamento financeiro.", description.getValue());
        assertEquals(List.of(), params.getValue().get("teeth"));
        assertEquals(12000L, params.getValue().get("priceCents"));
    }

    @Test
    void examWithTeethRejected() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("contactId",
                contactId.toString(), "catalogItemId", examId.toString(), "teeth", List.of(11))));
        verifyNoProposal();
    }

    @Test
    void invalidToothRejected() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("contactId",
                contactId.toString(), "catalogItemId", facetaId.toString(), "teeth", List.of(11, 19))));
        verifyNoProposal();
    }

    @Test
    void negativePriceRejected() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("contactId",
                contactId.toString(), "catalogItemId", facetaId.toString(), "priceCents", -1)));
        verifyNoProposal();
    }

    @Test
    void fractionalPriceRejectedAskingForCents() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx,
                Map.of("contactId", contactId.toString(), "catalogItemId", facetaId.toString(),
                        "priceCents", 750.5)));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("centavos"));
        verifyNoProposal();
    }

    @Test
    void unknownCatalogItemRejected() {
        UUID unknown = UUID.randomUUID();
        when(catalog.getById(companyId, unknown)).thenThrow(new CatalogItemNotFoundException(unknown));
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("contactId",
                contactId.toString(), "catalogItemId", unknown.toString())));
        verifyNoProposal();
    }

    @Test
    void contactOfOtherCompanyRejected() {
        UUID foreign = UUID.randomUUID();
        ContactResponse other = mock(ContactResponse.class);
        when(other.companyId()).thenReturn(UUID.randomUUID());
        when(contacts.getById(companyId, foreign)).thenReturn(other);
        assertThrows(IllegalArgumentException.class, () -> tool.execute(ctx, Map.of("contactId",
                foreign.toString(), "catalogItemId", facetaId.toString())));
        verifyNoProposal();
    }

    @Test
    void executorCreatesOneProcedurePerToothWithoutFinance() {
        PatientProcedureUseCase procedures = mock(PatientProcedureUseCase.class);
        AiActionExecutor executor = new AiActionExecutor(mock(TaskUseCase.class), mock(ActivityUseCase.class),
                mock(OpportunityUseCase.class), mock(AppointmentUseCase.class),
                mock(FinancialTransactionService.class), mock(InventoryService.class), contacts, procedures);
        AiAction action = mock(AiAction.class);
        when(action.getTool()).thenReturn("create_procedure");
        when(action.getCompanyId()).thenReturn(companyId);
        when(action.getUserId()).thenReturn(userId);
        when(action.getParameters()).thenReturn(Map.of("contactId", contactId.toString(),
                "catalogItemId", facetaId.toString(), "dentition", "PERMANENT", "teeth", List.of(11, 12),
                "priceCents", 75000, "state", "PLANNED"));

        executor.execute(action);

        ArgumentCaptor<PatientProcedureRequest> request = ArgumentCaptor.forClass(PatientProcedureRequest.class);
        verify(procedures, times(2)).create(eq(companyId), eq(contactId), request.capture(), eq(userId),
                eq(false));
        List<PatientProcedureRequest> all = request.getAllValues();
        assertEquals(11, all.get(0).teeth().get(0).tooth());
        assertEquals(12, all.get(1).teeth().get(0).tooth());
        for (PatientProcedureRequest r : all) {
            assertEquals(75000L, r.priceCents());
            assertEquals(facetaId, r.catalogItemId());
            assertEquals(ProcedureState.PLANNED, r.state());
            assertEquals(Dentition.PERMANENT, r.dentition());
            assertEquals(1, r.teeth().size());
            assertFalse(r.generateTransaction());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchCatalogListsActiveServices() {
        CatalogItemResponse faceta = item(facetaId, "Faceta em resina composta", "Estética", "750.00");
        when(catalog.list(companyId, "faceta", CatalogItemType.SERVICE, true, 0, 20))
                .thenReturn(PageResponse.of(List.of(faceta), 0, 20, 1));
        SearchCatalogTool search = new SearchCatalogTool(catalog);

        AiToolResult result = search.execute(new AiToolContext(companyId, userId,
                new AiPermissionContext(List.of("catalog:read")), conversationId),
                Map.of("query", "faceta"));

        assertTrue(result.success());
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.data();
        assertEquals(facetaId, data.get(0).get("id"));
        assertEquals("Estética", data.get(0).get("category"));
    }

    private void verifyNoProposal() {
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(),
                anyString());
    }
}
