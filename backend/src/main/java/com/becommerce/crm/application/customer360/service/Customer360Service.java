package com.becommerce.crm.application.customer360.service;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.customer360.dto.ContactSummaryResponse;
import com.becommerce.crm.application.customer360.dto.Customer360Response;
import com.becommerce.crm.application.customer360.dto.NextActionResponse;
import com.becommerce.crm.application.customer360.dto.OpportunityItemResponse;
import com.becommerce.crm.application.customer360.dto.TaskItemResponse;
import com.becommerce.crm.application.customer360.dto.TimelineEventResponse;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.domain.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Customer 360 (Sprint 13): visão consolidada de um contato — dados principais,
 * contexto comercial (oportunidades abertas e valor), tarefas, linha do tempo
 * unificada e a próxima ação recomendada (determinística, sem IA).
 *
 * <p>A pontuação de risco e a recomendação de próxima ação reutilizam a mesma
 * lógica de "oportunidade parada" do dashboard operacional.
 */
@Service
public class Customer360Service {

    /** Um contato é considerado "sem interação" após esse número de dias. */
    static final long STALE_DAYS = 7L;

    /** Limite de eventos da linha do tempo retornados. */
    static final int TIMELINE_LIMIT = 50;

    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final StageRepository stageRepository;
    private final PipelineRepository pipelineRepository;
    private final ActivityRepository activityRepository;
    private final TaskRepository taskRepository;

    public Customer360Service(ContactRepository contactRepository,
                              OpportunityRepository opportunityRepository,
                              StageRepository stageRepository,
                              PipelineRepository pipelineRepository,
                              ActivityRepository activityRepository,
                              TaskRepository taskRepository) {
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.pipelineRepository = pipelineRepository;
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public Customer360Response build(UUID companyId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .filter(c -> c.getCompanyId().equals(companyId) && c.isActive())
                .orElseThrow(() -> new ContactNotFoundException(contactId));

        var stages = stageRepository.findByCompanyId(companyId);
        Map<UUID, Stage> stageById = stages.stream().collect(Collectors.toMap(Stage::getId, Function.identity()));

        var pipelines = pipelineRepository.findByCompanyId(companyId);
        Map<UUID, Pipeline> pipelineById = pipelines.stream().collect(Collectors.toMap(Pipeline::getId, Function.identity()));

        List<Opportunity> opportunities = opportunityRepository.findByContactId(contactId);
        List<Task> tasks = taskRepository.findByContactId(contactId);

        List<Opportunity> open = opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.OPEN)
                .toList();

        var openValue = open.stream()
                .map(Opportunity::getValue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastInteraction = lastInteractionAt(contact, open, now);

        List<TaskItemResponse> taskItems = buildTasks(tasks, now);
        List<TimelineEventResponse> timeline = buildTimeline(opportunities, tasks, stageById,
                historyByOpportunity(opportunities), now);
        NextActionResponse nextAction = recommend(open, tasks, taskItems, lastInteraction, stageById, now);

        boolean atRisk = hasStaleOpen(open, lastInteraction, now);
        ContactSummaryResponse summary = new ContactSummaryResponse(
                contact.getId(),
                fullName(contact),
                contact.getEmail(),
                contact.getPhone(),
                contact.getNotes(),
                initials(contact),
                contact.getCreatedAt(),
                lastInteraction,
                atRisk,
                atRisk ? riskMessage(open, lastInteraction, now) : null);

        return new Customer360Response(
                companyId,
                summary,
                open.size(),
                openValue,
                buildOpportunities(opportunities, stageById, pipelineById),
                taskItems,
                timeline,
                nextAction);
    }

    // ------------------------------------------------------------------
    // Dados comerciais
    // ------------------------------------------------------------------

    private List<OpportunityItemResponse> buildOpportunities(List<Opportunity> opportunities,
                                                             Map<UUID, Stage> stageById,
                                                             Map<UUID, Pipeline> pipelineById) {
        return opportunities.stream()
                .sorted(Comparator.comparing(Opportunity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(o -> {
                    Stage stage = stageById.get(o.getStageId());
                    Pipeline pipeline = pipelineById.get(o.getPipelineId());
                    return new OpportunityItemResponse(
                            o.getId(),
                            o.getTitle(),
                            o.getValue(),
                            stage != null ? stage.getName() : "—",
                            stage != null ? stage.getProbability() : 0,
                            o.getStatus(),
                            statusLabel(o.getStatus()),
                            pipeline != null ? pipeline.getName() : "—",
                            o.getAssignedTo(),
                            o.getExpectedCloseDate());
                })
                .toList();
    }

    private List<TaskItemResponse> buildTasks(List<Task> tasks, LocalDateTime now) {
        List<TaskItemResponse> pending = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS)
                .map(t -> toTaskItem(t, now))
                .sorted(Comparator.comparing(TaskItemResponse::dueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toCollection(ArrayList::new));

        List<TaskItemResponse> finished = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .map(t -> toTaskItem(t, now))
                .sorted(Comparator.comparing(TaskItemResponse::completedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();

        pending.addAll(finished);
        return pending;
    }

    private TaskItemResponse toTaskItem(Task t, LocalDateTime now) {
        boolean overdue = (t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS)
                && t.getDueAt() != null && t.getDueAt().isBefore(now);
        return new TaskItemResponse(
                t.getId(), t.getTitle(), t.getStatus(), t.getPriority(),
                t.getDueAt(), t.getAssigneeId(), t.getCompletedAt(), overdue);
    }

    // ------------------------------------------------------------------
    // Linha do tempo
    // ------------------------------------------------------------------

    /**
     * Histórico de estágio de todas as oportunidades, em uma única consulta em
     * lote (evita N+1: antes era uma query por oportunidade).
     */
    private Map<UUID, List<OpportunityHistory>> historyByOpportunity(List<Opportunity> opportunities) {
        if (opportunities.isEmpty()) {
            return Map.of();
        }
        return opportunityRepository.findHistoryByOpportunityIds(
                opportunities.stream().map(Opportunity::getId).toList());
    }

    private List<TimelineEventResponse> buildTimeline(List<Opportunity> opportunities,
                                                      List<Task> tasks,
                                                      Map<UUID, Stage> stageById,
                                                      Map<UUID, List<OpportunityHistory>> historyByOpp,
                                                      LocalDateTime now) {
        List<TimelineEventResponse> events = new ArrayList<>();

        // Atividades diretamente vinculadas ao contato.
        for (var a : activityRepository.findByContactId(contactIdOf(opportunities, tasks))) {
            String typeLabel = a.getType() != null ? a.getType().name() : "ATIVIDADE";
            String subject = a.getSubject() != null ? a.getSubject() : a.getType() != null ? a.getType().name() : "Atividade";
            events.add(new TimelineEventResponse(
                    a.getId(), "ACTIVITY", "Atividade registrada: " + subject,
                    a.getDescription(), a.getActivityAt() != null ? a.getActivityAt() : a.getCreatedAt(),
                    a.getId(), subject));
        }

        // Oportunidades e seus movimentos de estágio.
        for (Opportunity o : opportunities) {
            events.add(new TimelineEventResponse(
                    o.getId(), "OPPORTUNITY_CREATED", "Oportunidade criada: " + o.getTitle(),
                    stageById.get(o.getStageId()) != null ? "Estágio inicial: " + stageById.get(o.getStageId()).getName() : null,
                    o.getCreatedAt(), o.getId(), o.getTitle()));

            for (OpportunityHistory h : historyByOpp.getOrDefault(o.getId(), List.of())) {
                String from = h.getFromStageId() != null && stageById.get(h.getFromStageId()) != null
                        ? stageById.get(h.getFromStageId()).getName() : null;
                String to = h.getToStageId() != null && stageById.get(h.getToStageId()) != null
                        ? stageById.get(h.getToStageId()).getName() : null;
                String desc = (from != null ? "De " + from : "") + (to != null ? " para " + to : "");
                events.add(new TimelineEventResponse(
                        h.getId(), "OPPORTUNITY_MOVED", "Oportunidade movida",
                        desc.isBlank() ? null : desc.trim(), h.getChangedAt(), o.getId(), o.getTitle()));
            }

            if (o.getStatus() == OpportunityStatus.WON && o.getWonAt() != null) {
                events.add(new TimelineEventResponse(
                        o.getId(), "OPPORTUNITY_WON", "Oportunidade ganha: " + o.getTitle(),
                        "Proposta convertida.", o.getWonAt(), o.getId(), o.getTitle()));
            }
            if (o.getStatus() == OpportunityStatus.LOST && o.getLostAt() != null) {
                events.add(new TimelineEventResponse(
                        o.getId(), "OPPORTUNITY_LOST", "Oportunidade perdida: " + o.getTitle(),
                        o.getLossReason(), o.getLostAt(), o.getId(), o.getTitle()));
            }
        }

        // Tarefas do contato.
        for (Task t : tasks) {
            events.add(new TimelineEventResponse(
                    t.getId(), "TASK_CREATED", "Tarefa criada: " + t.getTitle(),
                    t.getDescription(), t.getCreatedAt(), t.getId(), t.getTitle()));
            if (t.getStatus() == TaskStatus.COMPLETED && t.getCompletedAt() != null) {
                events.add(new TimelineEventResponse(
                        t.getId(), "TASK_COMPLETED", "Tarefa concluída: " + t.getTitle(),
                        null, t.getCompletedAt(), t.getId(), t.getTitle()));
            }
        }

        return events.stream()
                .sorted(Comparator.comparing(TimelineEventResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TIMELINE_LIMIT)
                .toList();
    }

    private UUID contactIdOf(List<Opportunity> opportunities, List<Task> tasks) {
        if (!opportunities.isEmpty()) {
            return opportunities.get(0).getContactId();
        }
        if (!tasks.isEmpty()) {
            return tasks.get(0).getContactId();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Interação, risco e próxima ação
    // ------------------------------------------------------------------

    private LocalDateTime lastInteractionAt(Contact contact, List<Opportunity> open, LocalDateTime now) {
        LocalDateTime latest = activityRepository.findLatestActivityAtByContactId(contact.getId()).orElse(null);
        if (!open.isEmpty()) {
            Map<UUID, LocalDateTime> latestByOpp = activityRepository
                    .findLatestActivityAtByOpportunityIds(open.stream().map(Opportunity::getId).toList());
            for (LocalDateTime l : latestByOpp.values()) {
                if (l != null && (latest == null || l.isAfter(latest))) {
                    latest = l;
                }
            }
        }
        return latest != null ? latest : contact.getCreatedAt();
    }

    private boolean hasStaleOpen(List<Opportunity> open, LocalDateTime lastInteraction, LocalDateTime now) {
        if (open.isEmpty()) {
            return false;
        }
        return Duration.between(lastInteraction, now).toDays() >= STALE_DAYS;
    }

    private String riskMessage(List<Opportunity> open, LocalDateTime lastInteraction, LocalDateTime now) {
        long days = Duration.between(lastInteraction, now).toDays();
        return "Sem interação há " + days + " dias com " + open.size()
                + " oportunidade(s) em aberto. Risco de esfriamento — agende um contato.";
    }

    private NextActionResponse recommend(List<Opportunity> open, List<Task> tasks,
                                         List<TaskItemResponse> taskItems, LocalDateTime lastInteraction,
                                         Map<UUID, Stage> stageById, LocalDateTime now) {
        // 1. Follow-up: alguma oportunidade aberta sem interação.
        if (!open.isEmpty()) {
            long days = Duration.between(lastInteraction, now).toDays();
            if (days >= STALE_DAYS) {
                return new NextActionResponse("FOLLOW_UP", "Agendar follow-up",
                        "Sem contato há " + days + " dias. Retome o relacionamento para evitar esfriamento.", 90);
            }
        }

        // 2. Tarefa vencida ou para hoje.
        TaskItemResponse urgent = taskItems.stream()
                .filter(t -> t.status() == TaskStatus.PENDING || t.status() == TaskStatus.IN_PROGRESS)
                .filter(t -> t.overdue() || (t.dueAt() != null && t.dueAt().isBefore(now.plusDays(1))))
                .findFirst().orElse(null);
        if (urgent != null) {
            String when = urgent.overdue() ? "está vencida" : "vence em breve";
            return new NextActionResponse("COMPLETE_TASK", "Concluir tarefa",
                    "A tarefa \"" + urgent.title() + "\" " + when + ". Priorize-a.", 80);
        }

        // 3. Fechamento próximo.
        Opportunity closing = open.stream()
                .filter(o -> o.getExpectedCloseDate() != null
                        && o.getExpectedCloseDate().minusDays(3).isBefore(now)
                        && o.getStatus() == OpportunityStatus.OPEN)
                .max(Comparator.comparing(Opportunity::getExpectedCloseDate))
                .orElse(null);
        if (closing != null) {
            return new NextActionResponse("REVIEW_CLOSING", "Revisar fechamento",
                    "A oportunidade \"" + closing.getTitle() + "\" tem fechamento próximo. Revise os próximos passos.", 60);
        }

        // 4. Alta probabilidade.
        Opportunity hot = open.stream()
                .filter(o -> stageById.get(o.getStageId()) != null
                        && stageById.get(o.getStageId()).getProbability() >= 70)
                .max(Comparator.comparing(Opportunity::getValue,
                        Comparator.nullsFirst(BigDecimal::compareTo)))
                .orElse(null);
        if (hot != null) {
            return new NextActionResponse("FORMAL_PROPOSAL", "Enviar proposta formal",
                    "Oportunidade de alta probabilidade. Formalize a proposta.", 40);
        }

        // 5. Sem prioridade.
        return new NextActionResponse("NONE", "Tudo em dia",
                "Nenhuma ação urgente para este contato no momento.", 0);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String fullName(Contact c) {
        return ((c.getFirstName() != null ? c.getFirstName() : "")
                + " " + (c.getLastName() != null ? c.getLastName() : "")).trim();
    }

    private static String initials(Contact c) {
        String first = c.getFirstName() != null && !c.getFirstName().isBlank()
                ? c.getFirstName().substring(0, 1) : "";
        String last = c.getLastName() != null && !c.getLastName().isBlank()
                ? c.getLastName().substring(0, 1) : "";
        return (first + last).isBlank() ? "?" : (first + last).toUpperCase();
    }

    private static String statusLabel(OpportunityStatus status) {
        if (status == null) return "ABERTA";
        return switch (status) {
            case OPEN -> "ABERTA";
            case WON -> "GANHA";
            case LOST -> "PERDIDA";
        };
    }
}
