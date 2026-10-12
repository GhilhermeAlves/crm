package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionExecutor;
import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.domain.AiAction;
import com.becommerce.crm.finance.transaction.application.dto.CreateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.dto.PayTransactionRequest;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.sales.activity.application.port.in.ActivityUseCase;
import com.becommerce.crm.sales.pipeline.application.port.in.OpportunityUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.task.application.port.in.TaskUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceWriteToolsTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), SP);

    private final UUID companyId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, userId,
            new AiPermissionContext(List.of("finance:manage")), conversationId);

    private final AiActionService actionService = mock(AiActionService.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final FinancialTransactionService transactions = mock(FinancialTransactionService.class);

    private final CreateTransactionTool createTool =
            new CreateTransactionTool(actionService, contactRepository, clock);
    private final PayTransactionTool payTool = new PayTransactionTool(actionService, transactions, clock);

    @BeforeEach
    void setUp() {
        when(actionService.propose(any(), any(), any(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(new AiActionResponse(UUID.randomUUID(), conversationId, "create_transaction",
                        "FINANCIAL_TRANSACTION", null, "Proposta", "PROPOSED", Map.of(), null, null,
                        LocalDateTime.now(), LocalDateTime.now()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> proposedParams(String tool) {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(actionService).propose(eq(companyId), eq(userId), eq(conversationId), eq(tool),
                eq("FINANCIAL_TRANSACTION"), any(), params.capture(), anyString());
        return params.getValue();
    }

    private FinancialTransactionResponse transaction(String description, long amountCents,
                                                     TransactionStatus status) {
        FinancialTransactionResponse response = mock(FinancialTransactionResponse.class);
        when(response.description()).thenReturn(description);
        when(response.amountCents()).thenReturn(amountCents);
        when(response.status()).thenReturn(status);
        return response;
    }

    @Test
    void createProposesIncomeAndConvertsAmountWithoutWriting() {
        var result = createTool.execute(ctx, Map.of(
                "type", "INCOME",
                "description", "Consulta Maria",
                "category", "CONSULTATION",
                "amount", "150.50",
                "paymentMethod", "PIX",
                "dueDate", "2026-10-12"));

        assertTrue(result.success());
        Map<String, Object> params = proposedParams("create_transaction");
        assertEquals("INCOME", params.get("type"));
        assertEquals("CONSULTATION", params.get("category"));
        assertEquals(15050L, params.get("amountCents"));
        assertEquals("PIX", params.get("paymentMethod"));
        assertEquals("2026-10-12", params.get("dueDate"));
        assertEquals(1, params.get("installments"));
        assertNull(params.get("paidAt"));
        verify(actionService).propose(companyId, userId, conversationId, "create_transaction",
                "FINANCIAL_TRANSACTION", null, params,
                "Lançar receita: Consulta Maria - R$ 150,50 (PIX, venc. 12/10/2026)");
        verify(transactions, never()).create(any(), any(), any());
        verify(transactions, never()).pay(any(), any(), any(), any());
    }

    @Test
    void createProposesInstallmentExpenseWithDefaultDate() {
        createTool.execute(ctx, Map.of(
                "type", "EXPENSE",
                "description", "Compra de insumos",
                "category", "SUPPLIES",
                "amount", "300",
                "installments", 3));

        Map<String, Object> params = proposedParams("create_transaction");
        assertEquals("EXPENSE", params.get("type"));
        assertEquals("PIX", params.get("paymentMethod"));
        assertEquals("2026-10-12", params.get("dueDate"));
        assertEquals(3, params.get("installments"));
        verify(actionService).propose(companyId, userId, conversationId, "create_transaction",
                "FINANCIAL_TRANSACTION", null, params,
                "Lançar despesa: Compra de insumos - R$ 300,00 (PIX, venc. 12/10/2026) em 3x");
    }

    @Test
    void createMarksPaidAtEqualsDueDateWhenPaid() {
        createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "Venda", "category", "PRODUCT_SALE",
                "amount", "50", "dueDate", "2026-10-20", "paid", true));

        assertEquals("2026-10-20", proposedParams("create_transaction").get("paidAt"));
    }

    @Test
    void createRejectsCategoryIncompatibleWithType() {
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "X", "category", "RENT", "amount", "10")));
        verify(actionService, never()).propose(any(), any(), any(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void createRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "X", "category", "CONSULTATION", "amount", "0")));
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "X", "category", "CONSULTATION", "amount", "-5")));
    }

    @Test
    void createRejectsMoreThanTwoDecimals() {
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "X", "category", "CONSULTATION", "amount", "150.555")));
    }

    @Test
    void createRejectsMissingDescriptionAndCategory() {
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "category", "CONSULTATION", "amount", "10")));
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "X", "amount", "10")));
    }

    @Test
    void createRejectsInvalidInstallments() {
        assertThrows(IllegalArgumentException.class, () -> createTool.execute(ctx, Map.of(
                "type", "INCOME", "description", "X", "category", "CONSULTATION", "amount", "10",
                "installments", 25)));
    }

    @Test
    void payProposesWithoutPaying() {
        UUID id = UUID.randomUUID();
        FinancialTransactionResponse open = transaction("Consulta Maria", 15000L, TransactionStatus.PENDING);
        when(transactions.getById(companyId, id)).thenReturn(open);

        var result = payTool.execute(ctx, Map.of("transactionId", id.toString(), "paidAt", "2026-10-15"));

        assertTrue(result.success());
        Map<String, Object> params = proposedParams("pay_transaction");
        assertEquals(id, params.get("transactionId"));
        assertEquals("2026-10-15", params.get("paidAt"));
        verify(actionService).propose(companyId, userId, conversationId, "pay_transaction",
                "FINANCIAL_TRANSACTION", id, params,
                "Dar baixa: Consulta Maria - R$ 150,00 em 15/10/2026");
        verify(transactions, never()).pay(any(), any(), any(), any());
    }

    @Test
    void payDefaultsPaidAtToToday() {
        UUID id = UUID.randomUUID();
        FinancialTransactionResponse open = transaction("Consulta", 1000L, TransactionStatus.PENDING);
        when(transactions.getById(companyId, id)).thenReturn(open);

        payTool.execute(ctx, Map.of("transactionId", id.toString()));

        assertEquals("2026-10-12", proposedParams("pay_transaction").get("paidAt"));
    }

    @Test
    void payRejectsNotOpenTransaction() {
        UUID paidId = UUID.randomUUID();
        FinancialTransactionResponse paid = transaction("Consulta", 1000L, TransactionStatus.PAID);
        when(transactions.getById(companyId, paidId)).thenReturn(paid);
        assertThrows(IllegalArgumentException.class, () ->
                payTool.execute(ctx, Map.of("transactionId", paidId.toString())));

        UUID canceledId = UUID.randomUUID();
        FinancialTransactionResponse canceled = transaction("Consulta", 1000L, TransactionStatus.CANCELLED);
        when(transactions.getById(companyId, canceledId)).thenReturn(canceled);
        assertThrows(IllegalArgumentException.class, () ->
                payTool.execute(ctx, Map.of("transactionId", canceledId.toString())));
    }

    @Test
    void payRejectsMissingTransactionId() {
        assertThrows(IllegalArgumentException.class, () -> payTool.execute(ctx, Map.of()));
    }

    @Test
    void executorCreatesAndPaysWithPersistedValues() {
        AiActionExecutor executor = new AiActionExecutor(mock(TaskUseCase.class), mock(ActivityUseCase.class),
                mock(OpportunityUseCase.class), mock(AppointmentUseCase.class), transactions,
                mock(InventoryService.class));
        UUID contactId = UUID.randomUUID();

        AiAction create = mock(AiAction.class);
        when(create.getTool()).thenReturn("create_transaction");
        when(create.getCompanyId()).thenReturn(companyId);
        when(create.getUserId()).thenReturn(userId);
        when(create.getParameters()).thenReturn(Map.of(
                "type", "INCOME", "description", "Consulta", "category", "CONSULTATION",
                "amountCents", 15050L, "paymentMethod", "PIX", "dueDate", "2026-10-12",
                "contactId", contactId.toString(), "installments", 1));
        executor.execute(create);
        verify(transactions).create(companyId, new CreateFinancialTransactionRequest(
                TransactionType.INCOME, "Consulta", TransactionCategory.CONSULTATION, 15050L, null,
                PaymentMethod.PIX, LocalDate.parse("2026-10-12"), contactId, null, null, null, null, 1), userId);

        UUID id = UUID.randomUUID();
        AiAction pay = mock(AiAction.class);
        when(pay.getTool()).thenReturn("pay_transaction");
        when(pay.getCompanyId()).thenReturn(companyId);
        when(pay.getUserId()).thenReturn(userId);
        when(pay.getParameters()).thenReturn(Map.of("transactionId", id.toString(), "paidAt", "2026-10-15"));
        executor.execute(pay);
        verify(transactions).pay(companyId, id,
                new PayTransactionRequest(LocalDate.parse("2026-10-15")), userId);
    }
}
