package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;

/**
 * Write tool {@code create_patient}: valida os dados, divide o nome completo em
 * primeiro nome + sobrenome, procura contatos com o mesmo nome completo, CPF ou
 * celular (avisa na descricao e devolve {@code duplicateOf} nos parametros) e
 * PERSISTE uma proposta. O cadastro so e feito apos confirmacao. Requer
 * {@code contact:create} (mesma do POST /patients).
 */
@Component
public class CreatePatientTool implements AiTool {

    static final String NAME = "create_patient";
    private static final int MAX_NAME = 100;
    private static final int MAX_NOTES = 500;
    private static final int SEARCH_LIMIT = 50;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AiActionService actionService;
    private final ContactUseCase contactUseCase;

    public CreatePatientTool(@Lazy AiActionService actionService, ContactUseCase contactUseCase) {
        this.actionService = actionService;
        this.contactUseCase = contactUseCase;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe CADASTRAR um paciente novo. Nao executa: cria uma proposta que o usuario confirma. "
                + "Divida o nome completo: firstName = primeiro nome, lastName = o restante. Se os parametros "
                + "da proposta trouxerem duplicateOf, ja existe paciente parecido: pergunte ao usuario se e "
                + "outra pessoa antes de seguir.";
    }

    @Override
    public String requiredPermission() {
        return "contact:create";
    }

    @Override
    public String risk() {
        return "WRITE";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("firstName", Map.of("type", "string", "description",
                "Primeiro nome (se vier o nome completo sem lastName, ele e dividido)"));
        props.put("lastName", Map.of("type", "string", "description", "Sobrenome (opcional)"));
        props.put("mobile", Map.of("type", "string", "description", "Celular (opcional)"));
        props.put("email", Map.of("type", "string", "description", "E-mail (opcional)"));
        props.put("birthDate", Map.of("type", "string", "description", "Nascimento AAAA-MM-DD (opcional)"));
        props.put("cpf", Map.of("type", "string", "description", "CPF (opcional)"));
        props.put("notes", Map.of("type", "string", "description", "Observacoes (opcional)"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("firstName"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        String[] name = splitName(text(args, "firstName"), text(args, "lastName"));
        String firstName = name[0];
        String lastName = name[1];
        if (firstName.length() > MAX_NAME || (lastName != null && lastName.length() > MAX_NAME)) {
            throw new IllegalArgumentException("Nome muito longo (maximo " + MAX_NAME + " caracteres).");
        }

        String email = trimmed(text(args, "email"));
        if (email != null && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
            throw new IllegalArgumentException("E-mail invalido.");
        }
        String mobile = trimmed(text(args, "mobile"));
        if (mobile != null && mobile.length() > 20) {
            throw new IllegalArgumentException("Celular invalido (maximo 20 caracteres).");
        }
        String cpf = trimmed(text(args, "cpf"));
        if (cpf != null && (cpf.length() > 14 || digits(cpf).length() != 11)) {
            throw new IllegalArgumentException("CPF invalido: informe os 11 digitos.");
        }
        LocalDate birthDate = parseBirthDate(trimmed(text(args, "birthDate")));
        String notes = trimmed(text(args, "notes"));
        if (notes != null && notes.length() > MAX_NOTES) {
            notes = notes.substring(0, MAX_NOTES);
        }

        String fullName = lastName == null ? firstName : firstName + " " + lastName;
        List<ContactResponse> duplicates = findDuplicates(ctx.companyId(), firstName, fullName, cpf, mobile);

        Map<String, Object> parameters = newMap();
        put(parameters, "firstName", firstName);
        put(parameters, "lastName", lastName);
        put(parameters, "mobile", mobile);
        put(parameters, "email", email);
        put(parameters, "birthDate", birthDate == null ? null : birthDate.toString());
        put(parameters, "cpf", cpf);
        put(parameters, "notes", notes);
        if (!duplicates.isEmpty()) {
            parameters.put("duplicateOf", duplicates.stream().map(c -> c.id().toString()).toList());
        }

        StringBuilder description = new StringBuilder("Cadastrar paciente: ").append(fullName);
        List<String> details = new ArrayList<>();
        if (mobile != null) {
            details.add("celular " + mobile);
        }
        if (email != null) {
            details.add("e-mail " + email);
        }
        if (birthDate != null) {
            details.add(String.format("nascimento %02d/%02d/%d",
                    birthDate.getDayOfMonth(), birthDate.getMonthValue(), birthDate.getYear()));
        }
        if (cpf != null) {
            details.add("CPF " + cpf);
        }
        if (!details.isEmpty()) {
            description.append(" (").append(String.join(", ", details)).append(")");
        }
        if (!duplicates.isEmpty()) {
            description.append(". ATENÇÃO: Já existe um paciente chamado ")
                    .append(String.join(", ", duplicates.stream().map(CreatePatientTool::fullName).toList()))
                    .append(" (mesmo nome, CPF ou celular); confirme só se for outra pessoa.");
        }

        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "CONTACT", null, parameters, description.toString());
        return AiToolResult.ok(NAME, proposal);
    }

    /** Nome completo em firstName (sem lastName) vira primeiro nome + restante. */
    static String[] splitName(String rawFirst, String rawLast) {
        String first = rawFirst == null ? "" : rawFirst.trim().replaceAll("\\s+", " ");
        String last = rawLast == null ? null : rawLast.trim().replaceAll("\\s+", " ");
        if (first.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome do paciente (firstName).");
        }
        if (last == null || last.isEmpty()) {
            int space = first.indexOf(' ');
            if (space > 0) {
                last = first.substring(space + 1);
                first = first.substring(0, space);
            } else {
                last = null;
            }
        }
        return new String[]{first, last};
    }

    private List<ContactResponse> findDuplicates(UUID companyId, String firstName, String fullName,
                                                 String cpf, String mobile) {
        Map<UUID, ContactResponse> found = new LinkedHashMap<>();
        String wanted = normalize(fullName);
        for (ContactResponse c : safeSearch(companyId, firstName)) {
            if (c.id() != null && normalize(fullName(c)).equals(wanted)) {
                found.putIfAbsent(c.id(), c);
            }
        }
        String cpfDigits = cpf == null ? "" : digits(cpf);
        String mobileDigits = mobile == null ? "" : digits(mobile);
        Set<String> queries = new LinkedHashSet<>();
        if (!cpfDigits.isEmpty()) {
            queries.add(cpfDigits);
        }
        if (mobileDigits.length() >= 8) {
            queries.add(mobileDigits);
        }
        for (String q : queries) {
            for (ContactResponse c : safeSearch(companyId, q)) {
                boolean sameCpf = !cpfDigits.isEmpty() && cpfDigits.equals(digits(c.cpf()));
                boolean sameMobile = mobileDigits.length() >= 8 && mobileDigits.equals(digits(c.mobile()));
                if (c.id() != null && (sameCpf || sameMobile)) {
                    found.putIfAbsent(c.id(), c);
                }
            }
        }
        return List.copyOf(found.values());
    }

    private List<ContactResponse> safeSearch(UUID companyId, String query) {
        try {
            List<ContactResponse> result = contactUseCase.search(companyId, query, SEARCH_LIMIT);
            return result == null ? List.of() : result;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static LocalDate parseBirthDate(String raw) {
        if (raw == null) {
            return null;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data de nascimento invalida: use AAAA-MM-DD.");
        }
        if (date.isAfter(LocalDate.now()) || date.getYear() < 1900) {
            throw new IllegalArgumentException("Data de nascimento invalida.");
        }
        return date;
    }

    private static String fullName(ContactResponse c) {
        String first = c.firstName() == null ? "" : c.firstName().trim();
        String last = c.lastName() == null ? "" : c.lastName().trim();
        return (first + " " + last).trim();
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String trimmed(String s) {
        return s == null ? null : s.trim();
    }
}
