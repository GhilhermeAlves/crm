package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.MoneyFormat;
import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.masterdata.catalog.application.dto.CatalogItemResponse;
import com.becommerce.crm.masterdata.catalog.application.service.CatalogService;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code create_procedure}: lanca um procedimento (com dentes FDI) ou
 * um exame (item de catalogo com categoria "exame", sem dentes) na ficha do
 * paciente. O preco e POR DENTE: na execucao cria um {@code PatientProcedure}
 * por dente, cada um com esse preco (o {@code priceCents} de um procedimento e o
 * valor total dele no orcamento/financeiro). Nunca gera financeiro. Requer
 * {@code contact:update} (mesma permissao do POST de procedimentos).
 */
@Component
public class CreateProcedureTool implements AiTool {

    static final String NAME = "create_procedure";
    private static final int MAX_NOTES = 5000;
    private static final int MAX_TEETH = 52;
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AiActionService actionService;
    private final ContactUseCase contactUseCase;
    private final CatalogService catalogService;
    private final Clock clock;

    @Autowired
    public CreateProcedureTool(@Lazy AiActionService actionService, ContactUseCase contactUseCase,
                               CatalogService catalogService) {
        this(actionService, contactUseCase, catalogService, Clock.systemUTC());
    }

    CreateProcedureTool(AiActionService actionService, ContactUseCase contactUseCase,
                        CatalogService catalogService, Clock clock) {
        this.actionService = actionService;
        this.contactUseCase = contactUseCase;
        this.catalogService = catalogService;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe LANCAR um procedimento ou exame na ficha do paciente. Nao executa: cria uma proposta que "
                + "o usuario confirma. Use search_contacts para o contactId e search_catalog para o "
                + "catalogItemId. Exames nao levam dentes. Se o item nao existir no catalogo, avise que "
                + "precisa ser cadastrado no catalogo primeiro. Nao gera lancamento financeiro.";
    }

    @Override
    public String requiredPermission() {
        return "contact:update";
    }

    @Override
    public String risk() {
        return "WRITE";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("contactId", Map.of("type", "string", "format", "uuid", "description",
                "id EXATO do paciente retornado por search_contacts; nunca invente"));
        props.put("catalogItemId", Map.of("type", "string", "format", "uuid", "description",
                "id EXATO do procedimento/exame retornado por search_catalog"));
        props.put("teeth", Map.of("type", "array", "items", Map.of("type", "integer"), "description",
                "Dentes na numeracao FDI (ex.: [11,12,21]); opcional; vazio para exames"));
        props.put("priceCents", Map.of("type", "integer", "description",
                "Valor POR DENTE em centavos (padrao: preco do catalogo)"));
        props.put("state", Map.of("type", "string", "enum", List.of("PLANNED", "DONE"), "description",
                "PLANNED (a realizar, padrao) ou DONE se o usuario disser que ja foi feito"));
        props.put("performedAt", Map.of("type", "string", "description",
                "Data de realizacao AAAA-MM-DD (so para DONE; padrao: hoje)"));
        props.put("notes", Map.of("type", "string", "description", "Observacoes (opcional)"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("contactId", "catalogItemId"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        UUID contactId = uuid(args, "contactId");
        if (contactId == null) {
            throw new IllegalArgumentException("Informe o contactId do paciente (use search_contacts).");
        }
        ContactResponse contact = findContact(ctx.companyId(), contactId);

        UUID itemId = uuid(args, "catalogItemId");
        if (itemId == null) {
            throw new IllegalArgumentException("Informe o catalogItemId (use search_catalog).");
        }
        CatalogItemResponse item = findItem(ctx.companyId(), itemId);
        boolean exam = isExam(item.category());

        List<Integer> teeth = teeth(args.get("teeth"));
        if (exam && !teeth.isEmpty()) {
            throw new IllegalArgumentException("Exames nao levam dentes: lance o exame sem informar dentes.");
        }
        Dentition dentition = dentition(teeth);

        long priceCents = price(args.get("priceCents"), item);

        ProcedureState state = state(text(args, "state"));
        LocalDate performedAt = null;
        if (state == ProcedureState.DONE) {
            String raw = text(args, "performedAt");
            performedAt = raw == null ? LocalDate.now(clock.withZone(AgendaFormat.CLINIC_ZONE)) : parseDate(raw);
        }

        String notes = text(args, "notes");
        if (notes != null) {
            notes = notes.trim();
            if (notes.length() > MAX_NOTES) {
                notes = notes.substring(0, MAX_NOTES);
            }
        }

        Map<String, Object> parameters = newMap();
        put(parameters, "contactId", contactId);
        put(parameters, "catalogItemId", itemId);
        put(parameters, "dentition", dentition.name());
        put(parameters, "teeth", teeth);
        put(parameters, "priceCents", priceCents);
        put(parameters, "state", state.name());
        put(parameters, "performedAt", performedAt == null ? null : performedAt.toString());
        put(parameters, "notes", notes);

        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "CONTACT", contactId, parameters,
                describe(exam, item.name(), teeth, priceCents, state, performedAt, fullName(contact)));
        return AiToolResult.ok(NAME, proposal);
    }

    static String describe(boolean exam, String itemName, List<Integer> teeth, long priceCents,
                           ProcedureState state, LocalDate performedAt, String patient) {
        int count = Math.max(teeth.size(), 1);
        StringBuilder d = new StringBuilder(exam ? "Lançar exame" : "Lançar procedimento");
        if (!patient.isBlank()) {
            d.append(" para ").append(patient);
        }
        d.append(": ").append(count).append(" × ").append(itemName);
        if (!teeth.isEmpty()) {
            d.append(teeth.size() == 1 ? " (dente " : " (dentes ").append(toothRanges(teeth)).append(")");
        }
        d.append(" — ").append(MoneyFormat.brl(priceCents));
        if (count > 1) {
            d.append(" cada, total ").append(MoneyFormat.brl(priceCents * count));
        }
        if (state == ProcedureState.DONE) {
            d.append(". Já realizado em ").append(performedAt.format(BR_DATE));
        } else {
            d.append(". A realizar");
        }
        d.append(". Sem lançamento financeiro.");
        return d.toString();
    }

    /** Agrupa dentes consecutivos do mesmo quadrante: [11..15, 21..25] vira "11–15, 21–25". */
    static String toothRanges(Collection<Integer> teeth) {
        List<String> parts = new ArrayList<>();
        Integer start = null;
        Integer prev = null;
        for (int t : new TreeSet<>(teeth)) {
            if (prev != null && t == prev + 1 && t / 10 == prev / 10) {
                prev = t;
                continue;
            }
            if (start != null) {
                parts.add(start.equals(prev) ? String.valueOf(start) : start + "–" + prev);
            }
            start = t;
            prev = t;
        }
        if (start != null) {
            parts.add(start.equals(prev) ? String.valueOf(start) : start + "–" + prev);
        }
        return String.join(", ", parts);
    }

    static boolean isExam(String category) {
        if (category == null) {
            return false;
        }
        String plain = Normalizer.normalize(category, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return plain.contains("exame");
    }

    private ContactResponse findContact(UUID companyId, UUID contactId) {
        ContactResponse contact;
        try {
            contact = contactUseCase.getById(companyId, contactId);
        } catch (RuntimeException e) {
            contact = null;
        }
        if (contact == null || (contact.companyId() != null && !contact.companyId().equals(companyId))) {
            throw new IllegalArgumentException("Paciente nao encontrado nesta clinica (use search_contacts).");
        }
        return contact;
    }

    private CatalogItemResponse findItem(UUID companyId, UUID itemId) {
        CatalogItemResponse item;
        try {
            item = catalogService.getById(companyId, itemId);
        } catch (RuntimeException e) {
            item = null;
        }
        if (item == null || !item.active() || item.type() != CatalogItemType.SERVICE
                || (item.companyId() != null && !item.companyId().equals(companyId))) {
            throw new IllegalArgumentException("Procedimento/exame nao encontrado no catalogo (use search_catalog). "
                    + "Se nao existir, o item precisa ser cadastrado no catalogo primeiro.");
        }
        return item;
    }

    private static List<Integer> teeth(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof Collection<?> values)) {
            throw new IllegalArgumentException("Informe os dentes como lista de numeros FDI (ex.: [11, 12]).");
        }
        TreeSet<Integer> teeth = new TreeSet<>();
        for (Object value : values) {
            try {
                teeth.add(value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value).trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Dente invalido: " + value + " (use a numeracao FDI, ex.: 11).");
            }
        }
        if (teeth.size() > MAX_TEETH) {
            throw new IllegalArgumentException("Dentes demais em um lancamento.");
        }
        return List.copyOf(teeth);
    }

    /** Permanente se todos forem permanentes; deciduo se todos forem deciduos; misto e recusado. */
    private static Dentition dentition(List<Integer> teeth) {
        if (teeth.stream().allMatch(Dentition.PERMANENT::isValidTooth)) {
            return Dentition.PERMANENT;
        }
        if (teeth.stream().allMatch(Dentition.DECIDUOUS::isValidTooth)) {
            return Dentition.DECIDUOUS;
        }
        List<Integer> invalid = teeth.stream()
                .filter(t -> !Dentition.PERMANENT.isValidTooth(t) && !Dentition.DECIDUOUS.isValidTooth(t))
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Dente(s) inexistente(s) na numeracao FDI: " + invalid + ".");
        }
        throw new IllegalArgumentException(
                "Nao misture dentes permanentes e deciduos no mesmo lancamento: faca um para cada denticao.");
    }

    private static long price(Object raw, CatalogItemResponse item) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            BigDecimal catalog = item.price();
            if (catalog == null) {
                throw new IllegalArgumentException("O item nao tem preco no catalogo: pergunte o valor por dente.");
            }
            return catalog.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        long cents;
        try {
            cents = new BigDecimal(String.valueOf(raw).trim()).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Valor com casas decimais: informe o preco em centavos, como inteiro (priceCents, ex.: 75000).");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor invalido: informe em centavos (ex.: 75000).");
        }
        if (cents < 0) {
            throw new IllegalArgumentException("O valor nao pode ser negativo.");
        }
        return cents;
    }

    private static ProcedureState state(String raw) {
        if (raw == null) {
            return ProcedureState.PLANNED;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "PLANNED" -> ProcedureState.PLANNED;
            case "DONE" -> ProcedureState.DONE;
            default -> throw new IllegalArgumentException("Estado invalido: use PLANNED ou DONE.");
        };
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data de realizacao invalida: use AAAA-MM-DD.");
        }
    }

    private static String fullName(ContactResponse contact) {
        String first = contact.firstName() == null ? "" : contact.firstName().trim();
        String last = contact.lastName() == null ? "" : contact.lastName().trim();
        return (first + " " + last).trim();
    }
}
