package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.MoneyFormat;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.decimal;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code create_transaction}: valida os argumentos (tipo, categoria
 * compativel, valor em reais, vencimento, vinculo de contato da MESMA empresa),
 * converte o valor para centavos e PERSISTE uma proposta
 * ({@code AiActionService#propose}). O lancamento so e criado apos confirmacao
 * explicita do usuario. Requer {@code finance:manage}.
 */
@Component
public class CreateTransactionTool implements AiTool {

    private static final String NAME = "create_transaction";
    private static final int MAX_DESCRIPTION = 255;
    private static final int MAX_INSTALLMENTS = 24;

    private final AiActionService actionService;
    private final ContactRepository contactRepository;
    private final Clock clock;

    @Autowired
    public CreateTransactionTool(@Lazy AiActionService actionService, ContactRepository contactRepository) {
        this(actionService, contactRepository, Clock.system(AgendaFormat.CLINIC_ZONE));
    }

    CreateTransactionTool(AiActionService actionService, ContactRepository contactRepository, Clock clock) {
        this.actionService = actionService;
        this.contactRepository = contactRepository;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe LANCAR uma receita ou despesa no financeiro. Nao executa: cria uma proposta "
                + "que o usuario deve confirmar. Informe o valor em reais (ex.: 150.50).";
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
                "type", Map.of("type", "string", "enum", List.of("INCOME", "EXPENSE"),
                        "description", "INCOME (receita) ou EXPENSE (despesa)"),
                "description", Map.of("type", "string", "description", "Descricao do lancamento (max 255)"),
                "category", Map.of("type", "string", "description",
                        "Categoria compativel com o tipo. Receita: PROCEDURE, CONSULTATION, PRODUCT_SALE, "
                                + "OTHER_INCOME. Despesa: RENT, PAYROLL, SUPPLIES, LAB, TAXES, MARKETING, "
                                + "UTILITIES, EQUIPMENT, OTHER_EXPENSE"),
                "amount", Map.of("type", "number", "description", "Valor em reais (ex.: 150.50)"),
                "paymentMethod", Map.of("type", "string", "description",
                        "Forma de pagamento (padrao PIX): BOLETO, CREDIT_CARD, DEBIT_CARD, PIX, CASH, "
                                + "TRANSFER, CHECK, OTHER"),
                "dueDate", Map.of("type", "string", "format", "date",
                        "description", "Vencimento AAAA-MM-DD (padrao: hoje)"),
                "paid", Map.of("type", "boolean",
                        "description", "true se ja foi pago (baixa na data de vencimento)"),
                "contactId", Map.of("type", "string", "format", "uuid",
                        "description", "Contato vinculado (opcional)"),
                "installments", Map.of("type", "integer", "minimum", 1, "maximum", MAX_INSTALLMENTS,
                        "description", "Parcelas (1 a 24, padrao 1)")));
        schema.put("required", List.of("type", "description", "category", "amount"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        TransactionType type = requiredType(args);
        String description = text(args, "description");
        if (description == null) {
            throw new IllegalArgumentException("A descrição do lançamento é obrigatória.");
        }
        if (description.length() > MAX_DESCRIPTION) {
            throw new IllegalArgumentException("A descrição deve ter no máximo 255 caracteres.");
        }
        TransactionCategory category = requiredCategory(args, type);
        long amountCents = amountCents(args);
        PaymentMethod paymentMethod = paymentMethod(args);
        LocalDate today = LocalDate.now(clock.withZone(AgendaFormat.CLINIC_ZONE));
        LocalDate dueDate = parseDate(text(args, "dueDate"), today);
        LocalDate paidAt = isTrue(args.get("paid")) ? dueDate : null;
        UUID contactId = uuid(args, "contactId");
        validateOwnedContact(ctx.companyId(), contactId);
        int installments = installments(args);

        Map<String, Object> parameters = newMap();
        put(parameters, "type", type.name());
        put(parameters, "description", description);
        put(parameters, "category", category.name());
        put(parameters, "amountCents", amountCents);
        put(parameters, "paymentMethod", paymentMethod.name());
        put(parameters, "dueDate", dueDate.toString());
        put(parameters, "paidAt", paidAt == null ? null : paidAt.toString());
        put(parameters, "contactId", contactId);
        put(parameters, "installments", installments);

        String summary = buildDescription(type, description, amountCents, paymentMethod, dueDate, installments);
        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "FINANCIAL_TRANSACTION", null, parameters, summary);
        return AiToolResult.ok(NAME, proposal);
    }

    private TransactionType requiredType(Map<String, Object> args) {
        String raw = text(args, "type");
        if (raw == null) {
            throw new IllegalArgumentException("Informe o tipo: INCOME (receita) ou EXPENSE (despesa).");
        }
        try {
            return TransactionType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo inválido: use INCOME ou EXPENSE.");
        }
    }

    private TransactionCategory requiredCategory(Map<String, Object> args, TransactionType type) {
        String raw = text(args, "category");
        if (raw == null) {
            throw new IllegalArgumentException("Informe a categoria do lançamento.");
        }
        TransactionCategory category;
        try {
            category = TransactionCategory.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Categoria inválida: " + raw + ".");
        }
        if (category.type() != type) {
            throw new IllegalArgumentException("A categoria " + category.name()
                    + " não é compatível com o tipo " + type.name() + ".");
        }
        return category;
    }

    private long amountCents(Map<String, Object> args) {
        BigDecimal amount = decimal(args, "amount");
        if (amount == null) {
            throw new IllegalArgumentException("Informe o valor do lançamento em reais.");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("O valor deve ter no máximo 2 casas decimais.");
        }
        return amount.movePointRight(2).longValueExact();
    }

    private PaymentMethod paymentMethod(Map<String, Object> args) {
        String raw = text(args, "paymentMethod");
        if (raw == null) {
            return PaymentMethod.PIX;
        }
        try {
            return PaymentMethod.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Forma de pagamento inválida: " + raw + ".");
        }
    }

    private int installments(Map<String, Object> args) {
        Object value = args.get("installments");
        if (value == null || String.valueOf(value).isBlank()) {
            return 1;
        }
        int installments;
        try {
            installments = new BigDecimal(String.valueOf(value)).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Número de parcelas inválido.");
        }
        if (installments < 1 || installments > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException("O número de parcelas deve estar entre 1 e " + MAX_INSTALLMENTS + ".");
        }
        return installments;
    }

    private void validateOwnedContact(UUID companyId, UUID contactId) {
        if (contactId == null) {
            return;
        }
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private static boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
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

    private static String buildDescription(TransactionType type, String description, long amountCents,
                                           PaymentMethod paymentMethod, LocalDate dueDate, int installments) {
        String label = type == TransactionType.INCOME ? "Lançar receita" : "Lançar despesa";
        StringBuilder sb = new StringBuilder(label).append(": ").append(description)
                .append(" - ").append(MoneyFormat.brl(amountCents))
                .append(" (").append(paymentMethod.name()).append(", venc. ")
                .append(AgendaFormat.date(dueDate)).append(")");
        if (installments > 1) {
            sb.append(" em ").append(installments).append("x");
        }
        return sb.toString();
    }
}
