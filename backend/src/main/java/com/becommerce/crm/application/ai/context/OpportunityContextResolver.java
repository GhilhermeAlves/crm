package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.ai.dto.AiFact;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.ai.AiRecordType;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.task.Task;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolver de contexto para {@code OPPORTUNITY} (AI-02), enriquecido pela
 * AI-06. Monta o contexto da oportunidade em foco com: dados principais,
 * responsável, atividades recentes, tarefas, histórico de estágio e tempo no
 * estágio atual. Exige {@code opportunity:read}.
 *
 * <p>O enriquecimento usa consultas diretas por recurso (atividades/tarefas/
 * histórico) — são consultas individuais para UMA oportunidade, sem loop por
 * item (sem N+1). {@link #facts(UUID, UUID)} devolve os dados como
 * {@link AiFact} estruturados (fonte {@code opportunity_context}) para a
 * análise contextual; {@link #resolve} os renderiza como texto para o chat.</p>
 */
@Component
public class OpportunityContextResolver implements AiRecordContextResolver {

    public static final String PERMISSION = "opportunity:read";

    static final int ACTIVITY_LIMIT = 10;
    static final int TASK_LIMIT = 10;
    static final int HISTORY_LIMIT = 10;

    private final OpportunityRepository opportunityRepository;
    private final StageRepository stageRepository;
    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public OpportunityContextResolver(OpportunityRepository opportunityRepository,
                                      StageRepository stageRepository,
                                      ContactRepository contactRepository,
                                      ActivityRepository activityRepository,
                                      TaskRepository taskRepository,
                                      UserRepository userRepository) {
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.contactRepository = contactRepository;
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AiRecordType type() {
        return AiRecordType.OPPORTUNITY;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    /**
     * Contexto textual para o chat (AI-02). {@code null} se o registro não
     * existir ou não for acessível via RLS.
     */
    @Override
    public String resolve(UUID companyId, UUID recordId) {
        OpportunityData data = gather(companyId, recordId);
        if (data == null) {
            return null;
        }
        return render(data);
    }

    /**
     * Fatos estruturados (AI-06) a partir de dado REAL do CRM. Vazio quando o
     * registro não for encontrado/acessível.
     */
    public List<AiFact> facts(UUID companyId, UUID recordId) {
        OpportunityData data = gather(companyId, recordId);
        if (data == null) {
            return List.of();
        }
        return buildFacts(data);
    }

    // ------------------------------------------------------------------
    // Coleta (uma consulta por recurso; sem loop por item)
    // ------------------------------------------------------------------

    private OpportunityData gather(UUID companyId, UUID recordId) {
        Optional<Opportunity> maybe = opportunityRepository.findById(recordId);
        if (maybe.isEmpty()) {
            return null;
        }
        Opportunity o = maybe.get();
        Stage stage = o.getStageId() != null
                ? stageRepository.findById(o.getStageId()).orElse(null) : null;
        Contact contact = o.getContactId() != null
                ? contactRepository.findById(o.getContactId()).orElse(null) : null;
        User assignee = o.getAssignedTo() != null
                ? userRepository.findById(o.getAssignedTo()).orElse(null) : null;

        List<Activity> activities = activityRepository.findByOpportunityId(recordId).stream()
                .filter(a -> a.getActivityAt() != null)
                .sorted(Comparator.comparing(Activity::getActivityAt).reversed())
                .limit(ACTIVITY_LIMIT)
                .toList();

        List<Task> tasks = taskRepository.findByCompanyIdAndOpportunityId(companyId, recordId).stream()
                .sorted(Comparator.comparing(Task::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TASK_LIMIT)
                .toList();

        List<OpportunityHistory> history = opportunityRepository.findHistoryByOpportunityId(recordId);
        Long daysInStage = daysInStage(history, o.getStageId());

        return new OpportunityData(o, stage, contact, assignee, activities, tasks, history, daysInStage);
    }

    /**
     * Tempo (em dias) no estágio atual. Determinado pelo movimento mais recente
     * para o estágio atual no histórico; se o histórico não permitir determinar
     * com segurança, retorna {@code null} (ausência do dado — não inventa).
     */
    private static Long daysInStage(List<OpportunityHistory> history, UUID currentStageId) {
        if (history == null || currentStageId == null) {
            return null;
        }
        OpportunityHistory last = null;
        for (OpportunityHistory h : history) {
            if (currentStageId.equals(h.getToStageId())) {
                last = h;
            }
        }
        if (last == null || last.getChangedAt() == null) {
            return null;
        }
        return Duration.between(last.getChangedAt(), LocalDateTime.now()).toDays();
    }

    // ------------------------------------------------------------------
    // Renderização textual (chat)
    // ------------------------------------------------------------------

    private String render(OpportunityData d) {
        Opportunity o = d.opportunity();
        StringBuilder sb = new StringBuilder();
        sb.append("Oportunidade: ").append(o.getTitle()).append('\n');
        sb.append("ID: ").append(o.getId()).append('\n');
        sb.append("Valor: R$ ").append(safe(o.getValue())).append('\n');
        sb.append("Status: ").append(statusLabel(o.getStatus())).append('\n');
        if (d.stage() != null) {
            sb.append("Estágio: ").append(d.stage().getName()).append('\n');
            sb.append("Probabilidade: ").append(d.stage().getProbability()).append("%").append('\n');
        }
        if (d.daysInStage() != null) {
            sb.append("Tempo no estágio: ").append(d.daysInStage()).append(" dia(s)\n");
        }
        if (o.getExpectedCloseDate() != null) {
            sb.append("Previsão de fechamento: ").append(o.getExpectedCloseDate().toLocalDate()).append('\n');
        }
        if (d.contact() != null) {
            sb.append("Contato vinculado: ").append(fullName(d.contact())).append('\n');
        }
        if (d.assignee() != null) {
            sb.append("Responsável: ").append(d.assignee().getName()).append('\n');
        } else if (o.getAssignedTo() != null) {
            sb.append("Responsável (id): ").append(o.getAssignedTo()).append('\n');
        }
        if (o.getLossReason() != null && !o.getLossReason().isBlank()) {
            sb.append("Motivo da perda: ").append(o.getLossReason()).append('\n');
        }
        if (o.getNotes() != null && !o.getNotes().isBlank()) {
            sb.append("Notas: ").append(o.getNotes()).append('\n');
        }

        if (!d.activities().isEmpty()) {
            sb.append("Atividades recentes:\n");
            for (Activity a : d.activities()) {
                sb.append("  - ").append(a.getType()).append(": ").append(a.getSubject())
                        .append(a.getActivityAt() != null ? " (" + a.getActivityAt().toLocalDate() + ")" : "")
                        .append('\n');
            }
        }
        if (!d.tasks().isEmpty()) {
            sb.append("Tarefas:\n");
            for (Task t : d.tasks()) {
                sb.append("  - ").append(t.getTitle()).append(" | ").append(taskStatusLabel(t))
                        .append('\n');
            }
        }
        if (!d.history().isEmpty()) {
            List<OpportunityHistory> recent = last(d.history(), HISTORY_LIMIT);
            sb.append("Histórico (últimos movimentos):\n");
            for (OpportunityHistory h : recent) {
                sb.append("  - ").append(h.getChangedAt().toLocalDate())
                        .append(": ").append(h.getFromStageId() != null ? h.getFromStageId() : "-")
                        .append(" → ").append(h.getToStageId() != null ? h.getToStageId() : "-")
                        .append('\n');
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Fatos estruturados (AI-06)
    // ------------------------------------------------------------------

    private List<AiFact> buildFacts(OpportunityData d) {
        Opportunity o = d.opportunity();
        String source = "opportunity_context";
        List<AiFact> facts = new ArrayList<>();

        facts.add(new AiFact("opportunity.id", "ID", String.valueOf(o.getId()), source));
        facts.add(new AiFact("opportunity.title", "Oportunidade", o.getTitle(), source));
        facts.add(new AiFact("opportunity.value", "Valor", "R$ " + safe(o.getValue()), source));
        facts.add(new AiFact("opportunity.status", "Status", statusLabel(o.getStatus()), source));
        if (d.stage() != null) {
            facts.add(new AiFact("opportunity.stage", "Estágio", d.stage().getName(), source));
            facts.add(new AiFact("opportunity.probability", "Probabilidade",
                    d.stage().getProbability() + "%", source));
        }
        if (d.daysInStage() != null) {
            facts.add(new AiFact("opportunity.time_in_stage", "Tempo no estágio",
                    d.daysInStage() + " dia(s)", source));
        }
        if (o.getExpectedCloseDate() != null) {
            facts.add(new AiFact("opportunity.expected_close", "Previsão de fechamento",
                    String.valueOf(o.getExpectedCloseDate().toLocalDate()), source));
        }
        if (d.contact() != null) {
            facts.add(new AiFact("opportunity.contact", "Contato vinculado",
                    fullName(d.contact()), source));
        }
        if (d.assignee() != null) {
            facts.add(new AiFact("opportunity.assignee", "Responsável",
                    d.assignee().getName(), source));
        } else if (o.getAssignedTo() != null) {
            facts.add(new AiFact("opportunity.assignee", "Responsável (id)",
                    String.valueOf(o.getAssignedTo()), source));
        }
        if (o.getLossReason() != null && !o.getLossReason().isBlank()) {
            facts.add(new AiFact("opportunity.loss_reason", "Motivo da perda",
                    o.getLossReason(), source));
        }
        if (o.getNotes() != null && !o.getNotes().isBlank()) {
            facts.add(new AiFact("opportunity.notes", "Notas", o.getNotes(), source));
        }
        if (!d.activities().isEmpty()) {
            facts.add(new AiFact("opportunity.activities", "Atividades recentes",
                    String.join(" | ", d.activities().stream()
                            .map(a -> a.getType() + ": " + a.getSubject()).toList()), source));
        }
        if (!d.tasks().isEmpty()) {
            facts.add(new AiFact("opportunity.tasks", "Tarefas",
                    String.join(" | ", d.tasks().stream()
                            .map(t -> t.getTitle() + " (" + taskStatusLabel(t) + ")").toList()), source));
        }
        if (!d.history().isEmpty()) {
            List<OpportunityHistory> recent = last(d.history(), HISTORY_LIMIT);
            facts.add(new AiFact("opportunity.history", "Histórico",
                    String.join(" | ", recent.stream()
                            .map(h -> h.getChangedAt().toLocalDate() + " -> " + h.getToStageId()).toList()),
                    source));
        }
        return facts;
    }

    private static List<OpportunityHistory> last(List<OpportunityHistory> history, int limit) {
        if (history.size() <= limit) {
            return history;
        }
        return history.subList(history.size() - limit, history.size());
    }

    private static String statusLabel(OpportunityStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case OPEN -> "ABERTA";
            case WON -> "GANHA";
            case LOST -> "PERDIDA";
        };
    }

    private static String taskStatusLabel(Task t) {
        return t.getStatus() != null ? t.getStatus().name() : "";
    }

    private static String fullName(Contact c) {
        return (c.getFirstName() + " " + (c.getLastName() == null ? "" : c.getLastName())).trim();
    }

    private static String safe(BigDecimal value) {
        return value == null ? "0,00" : value.toPlainString();
    }

    private record OpportunityData(Opportunity opportunity, Stage stage, Contact contact,
                                   User assignee, List<Activity> activities, List<Task> tasks,
                                   List<OpportunityHistory> history, Long daysInStage) {
    }
}