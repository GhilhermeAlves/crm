package com.becommerce.crm.automation.ai.application.agent.tool;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.sales.followup.application.dto.FollowUpRequest;
import com.becommerce.crm.sales.followup.application.dto.FollowUpResponse;
import com.becommerce.crm.sales.followup.application.port.in.FollowUpUseCase;
import com.becommerce.crm.shared.calendar.BrazilianHolidays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code agendar_lembrete_remarcacao}: quando o paciente desiste de remarcar,
 * o agente agenda UM lembrete (follow-up de WhatsApp) para retomar o
 * agendamento. Sai {@value #DAYS_LATER} dias depois, às 10h de Brasília, e
 * nunca em fim de semana ou feriado nacional (pula para o próximo dia útil).
 * Se o paciente responder antes, o follow-up é cancelado pela regra
 * {@code SUPERSEDED_BY_NEW_MESSAGE}.
 */
@Component
public class RescheduleReminderToolProvider implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(RescheduleReminderToolProvider.class);

    static final String REMIND = "agendar_lembrete_remarcacao";
    static final int DAYS_LATER = 2;
    static final LocalTime SEND_AT = LocalTime.of(10, 0);
    static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_MESSAGE_CHARS = 600;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final FollowUpUseCase followUpUseCase;
    private final Clock clock;

    @Autowired
    public RescheduleReminderToolProvider(FollowUpUseCase followUpUseCase) {
        this(followUpUseCase, Clock.systemDefaultZone());
    }

    RescheduleReminderToolProvider(FollowUpUseCase followUpUseCase, Clock clock) {
        this.followUpUseCase = followUpUseCase;
        this.clock = clock;
    }

    @Override
    public String id() {
        return "reschedule-reminder";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return session.conversationId() != null;
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "mensagem", Map.of("type", "string", "description",
                                "Mensagem do lembrete, curta e calorosa, chamando o paciente pelo primeiro nome "
                                        + "e retomando o agendamento. Ex.: Oi, Maria! Passando para ver se "
                                        + "conseguimos remarcar sua avaliação com a Dra. Raquel. Quer que eu "
                                        + "veja os horários desta semana?")),
                "required", List.of("mensagem"));
        return List.of(new AiProvider.ToolDefinition(REMIND,
                "Agenda UM lembrete por WhatsApp para retomar o agendamento quando o paciente desmarcou ou "
                        + "não conseguiu marcar e, depois de você insistir, decidiu deixar para depois.", schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("""
                Lembrete de remarcação (agendar_lembrete_remarcacao):
                - Use só quando o paciente desmarcou ou não conseguiu marcar e, mesmo depois de você oferecer horários concretos duas vezes, preferiu deixar para depois.
                - Agende uma única vez por conversa. O sistema escolhe a data (dia útil, às 10h) \
                — avise o paciente de forma leve, sem citar dia exato. Ex.: "Combinado! Daqui a uns dias te chamo pra gente ver um horário, tá?".
                - Não use para quem disse que não quer mais o atendimento.""");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        try {
            Map<String, Object> args = call.arguments() == null ? Map.of() : call.arguments();
            String message = args.get("mensagem") == null ? "" : String.valueOf(args.get("mensagem")).trim();
            if (message.isBlank()) {
                return "Lembrete não agendado: informe a mensagem do lembrete.";
            }
            if (message.length() > MAX_MESSAGE_CHARS) {
                message = message.substring(0, MAX_MESSAGE_CHARS);
            }
            LocalDate sendDay = nextBusinessDay(LocalDate.now(clock.withZone(CLINIC_ZONE)).plusDays(DAYS_LATER));
            LocalDateTime executeAt = sendDay.atTime(SEND_AT).atZone(CLINIC_ZONE)
                    .withZoneSameInstant(clock.getZone()).toLocalDateTime();
            // Uma chave por conversa e dia de envio: repetir a chamada não cria lembrete duplicado.
            UUID idempotencyKey = UUID.nameUUIDFromBytes(("reschedule-reminder:" + session.conversationId() + ":"
                    + sendDay).getBytes(StandardCharsets.UTF_8));
            FollowUpResponse created = followUpUseCase.create(session.companyId(),
                    new FollowUpRequest(session.conversationId(), message, executeAt, idempotencyKey, null));
            log.info("[AGENT][REMINDER] lembrete {} agendado para {} (company={}, conversation={})",
                    created.id(), sendDay, session.companyId(), session.conversationId());
            return "Lembrete agendado para " + sendDay.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR) + ", "
                    + sendDay.format(DateTimeFormatter.ofPattern("dd/MM", PT_BR))
                    + ", às 10h. Avise o paciente de forma leve, sem prometer o dia exato.";
        } catch (RuntimeException e) {
            log.warn("[AGENT][REMINDER] falha ao agendar lembrete (company={}): {}", session.companyId(),
                    e.getMessage());
            return "Lembrete não agendado. Não prometa retorno ao paciente.";
        }
    }

    /** Primeiro dia a partir de {@code day} que não é sábado, domingo nem feriado nacional. */
    static LocalDate nextBusinessDay(LocalDate day) {
        LocalDate d = day;
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY
                || BrazilianHolidays.isHoliday(d)) {
            d = d.plusDays(1);
        }
        return d;
    }
}
