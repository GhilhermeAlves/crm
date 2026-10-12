package com.becommerce.crm.automation.ai.application.tool.tools;

import com.becommerce.crm.automation.ai.application.tool.AbstractAiReadTool;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.MoneyFormat;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code search_transactions}: lista lançamentos financeiros (receitas e
 * despesas) de um período, com filtros opcionais de tipo, status e categoria.
 * Datas em {@code AAAA-MM-DD} (padrão: mês corrente). Fornece o
 * {@code transactionId} usado por {@code pay_transaction}. Exige
 * {@code finance:read}.
 */
@Component
public class SearchTransactionsTool extends AbstractAiReadTool {

    private static final String NAME = "search_transactions";

    private final FinancialTransactionService transactionService;
    private final Clock clock;

    @Autowired
    public SearchTransactionsTool(FinancialTransactionService transactionService) {
        this(transactionService, Clock.system(AgendaFormat.CLINIC_ZONE));
    }

    SearchTransactionsTool(FinancialTransactionService transactionService, Clock clock) {
        super(NAME, "Busca lançamentos financeiros (receitas/despesas) de um período, com filtros "
                        + "opcionais de tipo, status e categoria. Datas no formato AAAA-MM-DD "
                        + "(padrão: mês atual). Use antes de dar baixa para obter o transactionId.",
                "finance:read",
                Map.of(
                        "type", Map.of("type", "string", "enum", List.of("INCOME", "EXPENSE"),
                                "description", "Tipo do lançamento: INCOME (receita) ou EXPENSE (despesa)"),
                        "status", stringProp("Status: PENDING, PAID, CANCELLED ou OVERDUE (atrasado)"),
                        "from", stringProp("Data inicial AAAA-MM-DD (padrão: primeiro dia do mês)"),
                        "to", stringProp("Data final AAAA-MM-DD (padrão: último dia do mês)"),
                        "category", Map.of("type", "string",
                                "description", "Categoria do lançamento (ex.: CONSULTATION, PRODUCT_SALE, RENT)"),
                        "limit", integerProp("Máximo de resultados (1 a 50, padrão 20)")));
        this.transactionService = transactionService;
        this.clock = clock;
    }

    public record TransactionSummary(UUID id, String description, String type, String category,
                                     String amount, String dueDate, String paidAt, String status) {
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        LocalDate today = LocalDate.now(clock.withZone(AgendaFormat.CLINIC_ZONE));
        LocalDate from = parseDate(string(arguments, "from"), today.withDayOfMonth(1));
        LocalDate to = parseDate(string(arguments, "to"), today.withDayOfMonth(today.lengthOfMonth()));
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à final.");
        }
        TransactionType type = parseType(string(arguments, "type"));
        TransactionCategory category = parseCategory(string(arguments, "category"));
        String status = string(arguments, "status");
        int limit = clampLimit(integer(arguments, "limit"));

        List<FinancialTransactionResponse> transactions = transactionService
                .list(ctx.companyId(), type, status, from, to, null, category, null, null, null, 0, limit)
                .content();
        List<TransactionSummary> summaries = transactions.stream()
                .map(t -> new TransactionSummary(t.id(), t.description(), t.type().name(),
                        t.category().name(), MoneyFormat.brl(t.amountCents()),
                        t.dueDate() == null ? null : t.dueDate().toString(),
                        t.paidAt() == null ? null : t.paidAt().toString(),
                        t.status().name()))
                .toList();
        return AiToolResult.ok(NAME, summaries);
    }

    private LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }

    private TransactionType parseType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return TransactionType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo inválido: use INCOME ou EXPENSE.");
        }
    }

    private TransactionCategory parseCategory(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return TransactionCategory.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Categoria inválida: " + raw + ".");
        }
    }
}
