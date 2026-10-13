package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.MoneyFormat;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionService;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code pay_transaction}: valida que o lancamento e da empresa
 * ativa e esta em aberto (PENDING), e PERSISTE uma proposta de baixa
 * (pagamento). A baixa so ocorre apos confirmacao explicita do usuario. Requer
 * {@code finance:manage}.
 */
@Component
public class PayTransactionTool implements AiTool {

    private static final String NAME = "pay_transaction";

    private final AiActionService actionService;
    private final FinancialTransactionService transactionService;
    private final Clock clock;

    @Autowired
    public PayTransactionTool(@Lazy AiActionService actionService,
                              FinancialTransactionService transactionService) {
        this(actionService, transactionService, Clock.system(AgendaFormat.CLINIC_ZONE));
    }

    PayTransactionTool(AiActionService actionService, FinancialTransactionService transactionService,
                       Clock clock) {
        this.actionService = actionService;
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe dar BAIXA (marcar como pago) em um lancamento financeiro. Nao executa: cria uma "
                + "proposta que o usuario confirma. Antes, use search_transactions para obter o transactionId.";
    }

    @Override
    public String requiredPermission() {
        return "finance:manage";
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
                "transactionId", Map.of("type", "string", "format", "uuid"),
                "paidAt", Map.of("type", "string", "format", "date",
                        "description", "Data do pagamento AAAA-MM-DD (padrao: hoje)")));
        schema.put("required", List.of("transactionId"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        UUID transactionId = uuid(args, "transactionId");
        if (transactionId == null) {
            throw new IllegalArgumentException("Informe o transactionId do lançamento.");
        }
        FinancialTransactionResponse transaction = transactionService.getById(ctx.companyId(), transactionId);
        if (transaction.status() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Este lançamento não está em aberto (status: "
                    + transaction.status() + ").");
        }
        LocalDate today = LocalDate.now(clock.withZone(AgendaFormat.CLINIC_ZONE));
        LocalDate paidAt = parseDate(text(args, "paidAt"), today);

        Map<String, Object> parameters = newMap();
        put(parameters, "transactionId", transactionId);
        put(parameters, "paidAt", paidAt.toString());

        String description = "Dar baixa: " + transaction.description() + " - "
                + MoneyFormat.brl(transaction.amountCents()) + " em " + AgendaFormat.date(paidAt);
        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "FINANCIAL_TRANSACTION", transactionId, parameters, description);
        return AiToolResult.ok(NAME, proposal);
    }

    private static LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }
}
