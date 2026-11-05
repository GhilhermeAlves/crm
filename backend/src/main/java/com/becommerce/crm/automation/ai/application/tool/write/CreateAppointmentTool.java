package com.becommerce.crm.automation.ai.application.tool.write;

import com.becommerce.crm.automation.ai.application.action.AiActionService;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AgendaFormat;
import com.becommerce.crm.automation.ai.application.tool.AiTool;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.automation.ai.application.tool.SchedulingProfessionals;
import com.becommerce.crm.automation.ai.application.tool.SchedulingProfessionals.Professional;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentTypeResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentTypeUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.application.port.in.BlockUseCase;
import com.becommerce.crm.sales.scheduling.domain.AppointmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.put;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.text;
import static com.becommerce.crm.automation.ai.application.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code create_appointment}: valida paciente, horario (Brasilia,
 * no futuro), responsavel e tipo; avisa na descricao se o horario nao esta
 * livre (mesma regra do agendamento normal, sem {@code force}) e PERSISTE uma
 * proposta. A consulta so e criada apos confirmacao. Requer {@code appointment:create}.
 */
@Component
public class CreateAppointmentTool implements AiTool {

    static final String NAME = "create_appointment";
    static final int DEFAULT_DURATION = 30;
    private static final int MAX_DURATION = 600;
    private static final int MAX_TITLE = 200;
    private static final int MAX_NOTES = 2000;

    private final AiActionService actionService;
    private final AppointmentUseCase appointmentUseCase;
    private final AppointmentTypeUseCase appointmentTypeUseCase;
    private final BlockUseCase blockUseCase;
    private final ContactUseCase contactUseCase;
    private final SchedulingProfessionals professionals;
    private final Clock clock;

    @Autowired
    public CreateAppointmentTool(@Lazy AiActionService actionService, AppointmentUseCase appointmentUseCase,
                                 AppointmentTypeUseCase appointmentTypeUseCase, BlockUseCase blockUseCase,
                                 ContactUseCase contactUseCase, SchedulingProfessionals professionals) {
        this(actionService, appointmentUseCase, appointmentTypeUseCase, blockUseCase, contactUseCase,
                professionals, Clock.systemUTC());
    }

    CreateAppointmentTool(AiActionService actionService, AppointmentUseCase appointmentUseCase,
                          AppointmentTypeUseCase appointmentTypeUseCase, BlockUseCase blockUseCase,
                          ContactUseCase contactUseCase, SchedulingProfessionals professionals, Clock clock) {
        this.actionService = actionService;
        this.appointmentUseCase = appointmentUseCase;
        this.appointmentTypeUseCase = appointmentTypeUseCase;
        this.blockUseCase = blockUseCase;
        this.contactUseCase = contactUseCase;
        this.professionals = professionals;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe MARCAR uma consulta para um paciente. Nao executa: cria uma proposta que o usuario "
                + "confirma. Antes, use search_contacts para obter o contactId; se houver mais de um "
                + "profissional, use search_professionals para o hostId; search_appointment_types para o tipo.";
    }

    @Override
    public String requiredPermission() {
        return "appointment:create";
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
        props.put("startAt", Map.of("type", "string", "description",
                "Inicio em horario de Brasilia, formato AAAA-MM-DDTHH:mm"));
        props.put("durationMinutes", Map.of("type", "integer", "description",
                "Duracao em minutos (padrao: duracao do tipo de consulta, senao 30)"));
        props.put("appointmentTypeId", Map.of("type", "string", "format", "uuid", "description",
                "Tipo de consulta (opcional), id de search_appointment_types"));
        props.put("hostId", Map.of("type", "string", "format", "uuid", "description",
                "Profissional responsavel (opcional), id de search_professionals"));
        props.put("title", Map.of("type", "string", "description", "Titulo (padrao: Consulta - <nome>)"));
        props.put("notes", Map.of("type", "string", "description", "Observacoes (opcional)"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("contactId", "startAt"));
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

        Instant start = parseLocal(text(args, "startAt"));
        if (!start.isAfter(clock.instant())) {
            throw new IllegalArgumentException("O horario da consulta precisa estar no futuro.");
        }

        UUID typeId = uuid(args, "appointmentTypeId");
        AppointmentTypeResponse type = typeId == null ? null : findType(ctx.companyId(), typeId);

        int duration = duration(args, type);
        Instant end = start.plusSeconds(duration * 60L);

        Professional host = resolveHost(ctx, uuid(args, "hostId"));

        String title = text(args, "title");
        title = title == null ? "Consulta - " + fullName(contact) : title.trim();
        if (title.length() > MAX_TITLE) {
            title = title.substring(0, MAX_TITLE);
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
        put(parameters, "hostId", host.id());
        put(parameters, "appointmentTypeId", typeId);
        put(parameters, "title", title);
        put(parameters, "startAt", start.toString());
        put(parameters, "endAt", end.toString());
        put(parameters, "notes", notes);

        StringBuilder description = new StringBuilder("Marcar consulta: ").append(title)
                .append(" em ").append(AgendaFormat.shortDateTime(start))
                .append(" (").append(duration).append(" min)");
        if (type != null) {
            description.append(" — ").append(type.name());
        }
        if (!host.name().isBlank()) {
            description.append(" com ").append(host.name());
        }
        if (!isFree(ctx.companyId(), host.id(), start, end)) {
            description.append(". ATENÇÃO: este horário não está livre na agenda");
            if (!host.name().isBlank()) {
                description.append(" de ").append(host.name());
            }
            description.append(" (já há consulta ou bloqueio); a confirmação vai falhar se continuar ocupado.");
        }

        AiActionResponse proposal = actionService.propose(ctx.companyId(), ctx.userId(), ctx.conversationId(),
                NAME, "CONTACT", contactId, parameters, description.toString());
        return AiToolResult.ok(NAME, proposal);
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

    private AppointmentTypeResponse findType(UUID companyId, UUID typeId) {
        AppointmentTypeResponse type;
        try {
            type = appointmentTypeUseCase.getById(companyId, typeId);
        } catch (RuntimeException e) {
            type = null;
        }
        if (type == null || !type.active()) {
            throw new IllegalArgumentException(
                    "Tipo de consulta nao encontrado ou inativo (use search_appointment_types).");
        }
        return type;
    }

    private static int duration(Map<String, Object> args, AppointmentTypeResponse type) {
        Object raw = args.get("durationMinutes");
        if (raw == null || String.valueOf(raw).isBlank()) {
            return type != null && type.durationMinutes() > 0 ? type.durationMinutes() : DEFAULT_DURATION;
        }
        int minutes;
        try {
            minutes = raw instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duracao invalida: informe os minutos (ex.: 30).");
        }
        if (minutes <= 0 || minutes > MAX_DURATION) {
            throw new IllegalArgumentException("Duracao invalida: use entre 1 e " + MAX_DURATION + " minutos.");
        }
        return minutes;
    }

    private Professional resolveHost(AiToolContext ctx, UUID hostId) {
        List<Professional> all = professionals.list(ctx.companyId());
        if (hostId != null) {
            return all.stream().filter(p -> p.id().equals(hostId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Profissional nao encontrado nesta clinica (use search_professionals)."));
        }
        Optional<Professional> self = all.stream().filter(p -> p.id().equals(ctx.userId())).findFirst();
        if (self.isPresent()) {
            return self.get();
        }
        if (all.size() == 1) {
            return all.get(0);
        }
        if (all.isEmpty()) {
            throw new IllegalArgumentException("Nenhum profissional disponivel para a agenda desta clinica.");
        }
        throw new IllegalArgumentException("Ha varios profissionais na clinica ("
                + all.stream().map(Professional::name).collect(Collectors.joining(", "))
                + "). Pergunte ao usuario com quem e a consulta e informe o hostId (use search_professionals).");
    }

    /** Mesma regra do AppointmentService sem force: consultas nao canceladas e bloqueios do responsavel. */
    private boolean isFree(UUID companyId, UUID hostId, Instant start, Instant end) {
        boolean busy = appointmentUseCase.list(companyId, start, end, List.of(hostId)).stream()
                .filter(a -> a.status() != AppointmentStatus.CANCELED)
                .filter(a -> hostId.equals(a.hostId()))
                .anyMatch(a -> a.startAt().isBefore(end) && a.endAt().isAfter(start));
        if (busy) {
            return false;
        }
        return blockUseCase.list(companyId, start, end, List.of(hostId)).stream()
                .filter(b -> hostId.equals(b.hostId()))
                .noneMatch(b -> b.startAt().isBefore(end) && b.endAt().isAfter(start));
    }

    private static String fullName(ContactResponse contact) {
        String first = contact.firstName() == null ? "" : contact.firstName().trim();
        String last = contact.lastName() == null ? "" : contact.lastName().trim();
        return (first + " " + last).trim();
    }

    private static Instant parseLocal(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Informe o horario da consulta (startAt).");
        }
        try {
            return LocalDateTime.parse(raw.trim()).atZone(AgendaFormat.CLINIC_ZONE).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Horario invalido: use AAAA-MM-DDTHH:mm.");
        }
    }
}
