package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.tools.FinanceSummaryTool.FinanceSummary;
import com.becommerce.crm.automation.ai.application.tool.tools.SearchTransactionsTool.TransactionSummary;
import com.becommerce.crm.finance.report.application.dto.SummaryResponse;
import com.becommerce.crm.finance.report.application.service.FinanceReportService;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceReadToolsTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    // 12/10/2026 09:00 em São Paulo
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), SP);

    private final UUID companyId = UUID.randomUUID();
    private final AiToolContext ctx = new AiToolContext(companyId, UUID.randomUUID(),
            new AiPermissionContext(List.of("finance:read")));

    private final FinancialTransactionService transactions = mock(FinancialTransactionService.class);
    private final FinanceReportService reports = mock(FinanceReportService.class);

    private final SearchTransactionsTool searchTool = new SearchTransactionsTool(transactions, clock);
    private final FinanceSummaryTool summaryTool = new FinanceSummaryTool(reports, clock);

    private FinancialTransactionResponse transaction(String description, TransactionType type,
                                                     TransactionCategory category, long amountCents,
                                                     LocalDate dueDate, LocalDate paidAt,
                                                     TransactionStatus status) {
        FinancialTransactionResponse response = mock(FinancialTransactionResponse.class);
        when(response.id()).thenReturn(UUID.randomUUID());
        when(response.description()).thenReturn(description);
        when(response.type()).thenReturn(type);
        when(response.category()).thenReturn(category);
        when(response.amountCents()).thenReturn(amountCents);
        when(response.dueDate()).thenReturn(dueDate);
        when(response.paidAt()).thenReturn(paidAt);
        when(response.status()).thenReturn(status);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<TransactionSummary> data(AiToolResult result) {
        assertTrue(result.success(), result.error());
        return (List<TransactionSummary>) result.data();
    }

    @Test
    void searchDefaultsToCurrentMonthPassesFiltersAndFormatsAmount() {
        FinancialTransactionResponse invoice = transaction("Consulta Maria", TransactionType.INCOME,
                TransactionCategory.CONSULTATION, 15000L, LocalDate.parse("2026-10-20"), null,
                TransactionStatus.PENDING);
        when(transactions.list(eq(companyId), eq(TransactionType.INCOME), isNull(),
                eq(LocalDate.parse("2026-10-01")), eq(LocalDate.parse("2026-10-31")), isNull(),
                eq(TransactionCategory.CONSULTATION), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(PageResponse.of(List.of(invoice), 0, 20, 1));

        List<TransactionSummary> result = data(searchTool.execute(ctx,
                Map.of("type", "INCOME", "category", "CONSULTATION")));

        assertEquals(1, result.size());
        TransactionSummary summary = result.get(0);
        assertEquals("Consulta Maria", summary.description());
        assertEquals("INCOME", summary.type());
        assertEquals("CONSULTATION", summary.category());
        assertEquals("R$ 150,00", summary.amount());
        assertEquals("2026-10-20", summary.dueDate());
        assertEquals("PENDING", summary.status());
    }

    @Test
    void searchUsesExplicitRangeAndClampsLimit() {
        when(transactions.list(eq(companyId), isNull(), isNull(), eq(LocalDate.parse("2026-09-01")),
                eq(LocalDate.parse("2026-09-30")), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(50))).thenReturn(PageResponse.of(List.of(), 0, 50, 0));

        AiToolResult result = searchTool.execute(ctx,
                Map.of("from", "2026-09-01", "to", "2026-09-30", "limit", 999));

        assertTrue(result.success());
        assertTrue(data(result).isEmpty());
    }

    @Test
    void searchRejectsInvalidType() {
        AiToolResult result = searchTool.execute(ctx, Map.of("type", "BOGUS"));

        assertFalse(result.success());
    }

    @Test
    void searchRejectsInvalidDate() {
        AiToolResult result = searchTool.execute(ctx, Map.of("from", "12/10/2026"));

        assertFalse(result.success());
        assertEquals("Data inválida: use o formato AAAA-MM-DD.", result.error());
    }

    @Test
    void summaryDefaultsToCurrentMonthAndFormatsAmounts() {
        when(reports.summary(eq(companyId), isNull(), isNull(), eq(LocalDate.parse("2026-10-01")),
                eq(LocalDate.parse("2026-10-31")), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new SummaryResponse(100000L, 20000L, 5000L, 3000L, 80000L));

        AiToolResult result = summaryTool.execute(ctx, Map.of());

        assertTrue(result.success(), result.error());
        FinanceSummary summary = (FinanceSummary) result.data();
        assertEquals("R$ 1.000,00", summary.receitasPagas());
        assertEquals("R$ 50,00", summary.receitasAbertas());
        assertEquals("R$ 200,00", summary.despesasPagas());
        assertEquals("R$ 30,00", summary.despesasAbertas());
        assertEquals("R$ 800,00", summary.saldoRealizado());
    }

    @Test
    void summaryUsesExplicitRange() {
        when(reports.summary(eq(companyId), isNull(), isNull(), eq(LocalDate.parse("2026-01-01")),
                eq(LocalDate.parse("2026-01-31")), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new SummaryResponse(0L, 0L, 0L, 0L, 0L));

        AiToolResult result = summaryTool.execute(ctx, Map.of("from", "2026-01-01", "to", "2026-01-31"));

        assertTrue(result.success());
        verify(reports).summary(companyId, null, null, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), null, null, null, null, null);
    }
}
