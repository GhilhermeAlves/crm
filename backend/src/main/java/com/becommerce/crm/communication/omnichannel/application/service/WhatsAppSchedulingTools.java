package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.sales.scheduling.application.dto.AppointmentResponse;
import com.becommerce.crm.sales.scheduling.application.dto.CreateAppointmentRequest;
import com.becommerce.crm.sales.scheduling.application.dto.SlotResponse;
import com.becommerce.crm.sales.scheduling.application.port.in.AppointmentUseCase;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentTypeRepository;
import com.becommerce.crm.sales.scheduling.application.service.SlotService;
import com.becommerce.crm.sales.scheduling.domain.AppointmentSource;
import com.becommerce.crm.sales.scheduling.domain.AppointmentType;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingNotFoundException;
import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import com.becommerce.crm.sales.scheduling.domain.exception.SlotUnavailableException;
import com.becommerce.crm.shared.calendar.BrazilianHolidays;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Ferramentas de agenda do agente de WhatsApp (tool calling): consultar
 * horários livres e agendar. A IA só oferece horários devolvidos pela agenda
 * real, e o agendamento revalida o horário (o banco ainda impede sobreposição
 * por profissional). Agendamentos ficam com origem {@link AppointmentSource#WHATSAPP}
 * e o profissional é avisado no sininho.
 */
@Service
public class WhatsAppSchedulingTools {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSchedulingTools.class);

    static final String CHECK_SLOTS = "consultar_horarios_livres";
    static final String BOOK = "agendar_consulta";

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final int MAX_RANGE_DAYS = 14;
    private static final int MAX_SLOTS_LISTED = 15;

    /** Dados da conversa necessários para agendar. */
    public record Context(UUID companyId, UUID conversationId, String phone, String senderName) {
    }

    private final AppointmentTypeRepository typeRepository;
    private final SlotService slotService;
    private final AppointmentUseCase appointmentUseCase;
    private final ContactRepository contactRepository;
    private final NotificationUseCase notificationUseCase;
    private final UserRepository userRepository;

    public WhatsAppSchedulingTools(AppointmentTypeRepository typeRepository, SlotService slotService,
                                   AppointmentUseCase appointmentUseCase, ContactRepository contactRepository,
                                   NotificationUseCase notificationUseCase, UserRepository userRepository) {
        this.typeRepository = typeRepository;
        this.slotService = slotService;
        this.appointmentUseCase = appointmentUseCase;
        this.contactRepository = contactRepository;
        this.notificationUseCase = notificationUseCase;
        this.userRepository = userRepository;
    }

    /** Tipos ativos com profissional responsável (sem eles a agenda não é oferecida à IA). */
    public List<AppointmentType> bookableTypes(UUID companyId) {
        return typeRepository.findByCompanyId(companyId).stream()
                .filter(AppointmentType::isActive)
                .filter(t -> !t.getHostIds().isEmpty())
                .toList();
    }

    /** Orientação para o modelo, com os tipos de consulta disponíveis. */
    public String guidance(List<AppointmentType> types) {
        StringBuilder sb = new StringBuilder("Agenda online disponível. Tipos de consulta:\n");
        for (AppointmentType t : types) {
            sb.append("- ").append(t.getName()).append(" (").append(formatDuration(t.getDurationMinutes())).append(")\n");
        }
        sb.append("""
                Regras da agenda (têm prioridade sobre instruções de que "a equipe vai confirmar o horário"):
                - Para saber horários, use SEMPRE a ferramenta consultar_horarios_livres; nunca invente ou suponha horários.
                - Ofereça no máximo 3 a 4 opções por vez, de forma natural.
                - Antes de agendar, confirme com o paciente o tipo de consulta, o dia/horário escolhido e o nome completo.
                - Para agendar, use agendar_consulta com o horário exatamente como veio da consulta.
                - Depois de agendar, confirme dia, horário e profissional. Se falhar, ofereça outros horários.""");
        return sb.toString();
    }

    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> checkSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "tipo_consulta", Map.of("type", "string", "description", "Nome do tipo de consulta"),
                        "data_inicial", Map.of("type", "string", "description", "Primeiro dia (AAAA-MM-DD); padrão: hoje"),
                        "data_final", Map.of("type", "string", "description", "Último dia (AAAA-MM-DD); padrão: 6 dias após a inicial"),
                        "periodo", Map.of("type", "string", "enum", List.of("manha", "tarde", "noite", "qualquer"))),
                "required", List.of("tipo_consulta"));
        Map<String, Object> bookSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "tipo_consulta", Map.of("type", "string", "description", "Nome do tipo de consulta"),
                        "inicio", Map.of("type", "string", "description", "Início exato AAAA-MM-DDTHH:mm (horário de Brasília), como veio de consultar_horarios_livres"),
                        "nome_paciente", Map.of("type", "string", "description", "Nome completo do paciente"),
                        "observacao", Map.of("type", "string", "description", "Motivo ou observação (opcional)")),
                "required", List.of("tipo_consulta", "inicio", "nome_paciente"));
        return List.of(
                new AiProvider.ToolDefinition(CHECK_SLOTS,
                        "Lista horários livres reais da agenda para um tipo de consulta em um intervalo de datas.", checkSchema),
                new AiProvider.ToolDefinition(BOOK,
                        "Agenda a consulta no horário escolhido pelo paciente (só horários vindos de consultar_horarios_livres).", bookSchema));
    }

    /** Executa a ferramenta e devolve o resultado em texto para o modelo. Nunca lança. */
    public String execute(Context ctx, AiProvider.ToolCall call) {
        try {
            return switch (call.name()) {
                case CHECK_SLOTS -> checkSlots(ctx, call.arguments());
                case BOOK -> book(ctx, call.arguments());
                default -> "Ferramenta desconhecida: " + call.name();
            };
        } catch (RuntimeException e) {
            log.warn("[WHATSAPP][AGENDA] falha na ferramenta {} (company={}): {}", call.name(), ctx.companyId(),
                    e.getMessage());
            return "Não foi possível acessar a agenda agora. Diga ao paciente que a equipe vai retornar para agendar.";
        } finally {
            // SlotService/AppointmentService limpam o TenantContext ao terminar.
            TenantContext.setCompanyId(ctx.companyId());
        }
    }

    private String checkSlots(Context ctx, Map<String, Object> args) {
        Optional<AppointmentType> type = findType(ctx.companyId(), str(args, "tipo_consulta"));
        if (type.isEmpty()) {
            return "Tipo de consulta não encontrado. Tipos disponíveis: " + typeNames(ctx.companyId());
        }
        LocalDate today = LocalDate.now(ZONE);
        LocalDate from = parseDate(str(args, "data_inicial")).filter(d -> !d.isBefore(today)).orElse(today);
        LocalDate to = parseDate(str(args, "data_final")).filter(d -> !d.isBefore(from)).orElse(from.plusDays(6));
        if (to.isAfter(from.plusDays(MAX_RANGE_DAYS))) {
            to = from.plusDays(MAX_RANGE_DAYS);
        }
        String period = Optional.ofNullable(str(args, "periodo")).orElse("qualquer");

        List<LocalDateTime> slots = slotService.getAvailableSlots(ctx.companyId(), type.get().getId(), from, to)
                .stream()
                .map(s -> LocalDateTime.ofInstant(s.start(), ZONE))
                .filter(start -> inPeriod(start, period))
                .toList();
        String holidays = holidayNote(from, to);
        if (slots.isEmpty()) {
            return "Sem horários livres para " + type.get().getName() + " entre " + br(from) + " e " + br(to)
                    + (period.equals("qualquer") ? "" : " no período da " + period)
                    + ". Sugira outro período ou outras datas." + holidays;
        }
        Map<LocalDate, List<String>> byDay = new LinkedHashMap<>();
        for (LocalDateTime start : slots.subList(0, Math.min(slots.size(), MAX_SLOTS_LISTED))) {
            byDay.computeIfAbsent(start.toLocalDate(), d -> new ArrayList<>()).add(start.format(INPUT));
        }
        StringBuilder sb = new StringBuilder("Horários livres para ").append(type.get().getName())
                .append(" (").append(formatDuration(type.get().getDurationMinutes())).append("):\n");
        byDay.forEach((day, starts) -> sb.append("- ").append(weekday(day)).append(" ").append(br(day)).append(": ")
                .append(String.join(", ", starts.stream().map(s -> s.substring(11)).toList()))
                .append("  [inicio para agendar: ").append(String.join(" | ", starts)).append("]\n"));
        if (slots.size() > MAX_SLOTS_LISTED) {
            sb.append("(há mais horários; refine por dia ou período se preciso)");
        }
        return sb.append(holidays).toString();
    }

    /** Feriados nacionais no período: o consultório não atende (sem horários nesses dias). */
    static String holidayNote(LocalDate from, LocalDate to) {
        List<BrazilianHolidays.Holiday> holidays = BrazilianHolidays.between(from, to);
        if (holidays.isEmpty()) {
            return "";
        }
        return "\nFeriados no período (sem atendimento): " + String.join(", ", holidays.stream()
                .map(h -> weekday(h.date()) + " " + br(h.date()) + " (" + h.name() + ")").toList()) + ".";
    }

    private String book(Context ctx, Map<String, Object> args) {
        Optional<AppointmentType> typeOpt = findType(ctx.companyId(), str(args, "tipo_consulta"));
        if (typeOpt.isEmpty()) {
            return "Tipo de consulta não encontrado. Tipos disponíveis: " + typeNames(ctx.companyId());
        }
        AppointmentType type = typeOpt.get();
        LocalDateTime localStart;
        try {
            localStart = LocalDateTime.parse(String.valueOf(str(args, "inicio")).trim(), INPUT);
        } catch (DateTimeParseException | NullPointerException e) {
            return "Horário inválido; use o formato AAAA-MM-DDTHH:mm vindo de consultar_horarios_livres.";
        }
        String patientName = Optional.ofNullable(str(args, "nome_paciente"))
                .orElse(ctx.senderName() != null ? ctx.senderName() : "Paciente");
        Instant start = localStart.atZone(ZONE).toInstant();
        Instant end = start.plusSeconds(type.getDurationMinutes() * 60L);

        // Revalida contra a agenda real (antecedência mínima, buffers, bloqueios).
        boolean stillFree = slotService.getAvailableSlots(ctx.companyId(), type.getId(),
                        localStart.toLocalDate(), localStart.toLocalDate()).stream()
                .map(SlotResponse::start)
                .anyMatch(start::equals);
        TenantContext.setCompanyId(ctx.companyId());
        if (!stillFree) {
            return "Esse horário não está mais disponível. Consulte os horários livres novamente e ofereça outras opções.";
        }

        UUID contactId = findContactId(ctx.companyId(), ctx.phone());
        String notes = "Agendado pelo assistente do WhatsApp. Telefone: " + formatPhone(ctx.phone())
                + Optional.ofNullable(str(args, "observacao")).map(o -> ". Observação: " + o).orElse("");
        String title = type.getName() + " — " + patientName;

        for (UUID hostId : type.getHostIds()) {
            try {
                AppointmentResponse created = appointmentUseCase.create(ctx.companyId(),
                        new CreateAppointmentRequest(type.getId(), hostId, contactId, null, title, start, end,
                                type.getLocationKind(), type.getLocationDetail(), notes, false),
                        null, AppointmentSource.WHATSAPP);
                TenantContext.setCompanyId(ctx.companyId());
                String hostName = hostName(hostId);
                notifyHost(ctx.companyId(), hostId, created.id(), type.getName(), patientName, localStart, ctx.phone());
                log.info("[WHATSAPP][AGENDA] company={} conversation={} agendamento {} criado",
                        ctx.companyId(), ctx.conversationId(), created.id());
                return "Agendado com sucesso: " + type.getName() + " em " + weekday(localStart.toLocalDate()) + ", "
                        + br(localStart.toLocalDate()) + " às " + localStart.format(DateTimeFormatter.ofPattern("HH:mm"))
                        + (hostName != null ? " com " + hostName : "") + ", para " + patientName + ".";
            } catch (SlotUnavailableException | SchedulingValidationException | DataIntegrityViolationException e) {
                TenantContext.setCompanyId(ctx.companyId());
                // Profissional ocupado nesse horário: tenta o próximo responsável do tipo.
            } catch (SchedulingNotFoundException e) {
                TenantContext.setCompanyId(ctx.companyId());
                break;
            }
        }
        return "Esse horário acabou de ser ocupado. Consulte os horários livres novamente e ofereça outras opções.";
    }

    private void notifyHost(UUID companyId, UUID hostId, UUID appointmentId, String typeName, String patient,
                            LocalDateTime start, String phone) {
        try {
            notificationUseCase.create(companyId, new CreateNotificationRequest(hostId, NotificationType.INFO,
                    "Nova consulta agendada pelo WhatsApp",
                    typeName + " — " + patient + " · " + weekday(start.toLocalDate()) + " " + br(start.toLocalDate())
                            + " às " + start.format(DateTimeFormatter.ofPattern("HH:mm")) + " · " + formatPhone(phone),
                    "{\"appointmentId\":\"" + appointmentId + "\"}"), new UUID(0L, 0L));
        } catch (RuntimeException e) {
            log.warn("[WHATSAPP][AGENDA] falha ao notificar profissional (company={}): {}", companyId, e.getMessage());
        } finally {
            TenantContext.setCompanyId(companyId);
        }
    }

    private Optional<AppointmentType> findType(UUID companyId, String requested) {
        List<AppointmentType> types = bookableTypes(companyId);
        if (types.size() == 1 && (requested == null || requested.isBlank())) {
            return Optional.of(types.get(0));
        }
        String wanted = normalize(requested);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        return types.stream().filter(t -> normalize(t.getName()).equals(wanted)).findFirst()
                .or(() -> types.stream().filter(t -> normalize(t.getName()).contains(wanted)
                        || wanted.contains(normalize(t.getName()))).findFirst());
    }

    private String typeNames(UUID companyId) {
        return String.join(", ", bookableTypes(companyId).stream().map(AppointmentType::getName).toList());
    }

    private UUID findContactId(UUID companyId, String phone) {
        if (phone == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        for (String variant : com.becommerce.crm.communication.omnichannel.domain.IgnoredContact.phoneVariants(phone)) {
            candidates.add(variant);
            candidates.add("+" + variant);
        }
        for (String candidate : candidates) {
            Optional<Contact> contact = contactRepository.findByCompanyIdAndPhone(companyId, candidate);
            if (contact.isPresent()) {
                return contact.get().getId();
            }
        }
        return null;
    }

    private String hostName(UUID hostId) {
        return userRepository.findById(hostId).map(User::getName).filter(n -> !n.isBlank()).orElse(null);
    }

    static boolean inPeriod(LocalDateTime start, String period) {
        int hour = start.getHour();
        return switch (normalize(period)) {
            case "manha" -> hour < 12;
            case "tarde" -> hour >= 12 && hour < 18;
            case "noite" -> hour >= 18;
            default -> true;
        };
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private static Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length()))));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String str(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v);
    }

    private static String weekday(LocalDate day) {
        return day.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
    }

    private static String br(LocalDate day) {
        return day.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    static String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) {
            return m + " min";
        }
        return m == 0 ? h + "h" : h + "h" + String.format("%02d", m);
    }

    private static String formatPhone(String digits) {
        if (digits != null && digits.matches("55\\d{10,11}")) {
            String local = digits.substring(4);
            int split = local.length() - 4;
            return "+55 (" + digits.substring(2, 4) + ") " + local.substring(0, split) + "-" + local.substring(split);
        }
        return digits == null ? "" : "+" + digits;
    }
}
