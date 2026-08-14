package com.becommerce.crm.application.dashboard.service;

import com.becommerce.crm.application.activity.dto.ActivityResponse;
import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.dashboard.dto.AttentionOpportunity;
import com.becommerce.crm.application.dashboard.dto.OperationalDashboard;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.application.task.dto.TaskResponse;
import com.becommerce.crm.application.task.port.output.TaskRepository;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.task.Task;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dashboard orientado à ação (Sprint 12, ITEMS 3 e 4).
 *
 * <p>Responde, de forma determinística (sem IA), à pergunta "o que merece
 * minha atenção hoje?": oportunidades paradas, follow-up recomendado,
 * tarefas com vencimento para hoje e os últimos registros de atividade.
 */
@Service
public class DashboardService {

    /** Uma oportunidade é considerada "parada" se não recebe atividade há 7+ dias. */
    static final long STALE_DAYS = 7L;

    /** Limite de oportunidades destacadas no dashboard. */
    static final int ATTENTION_LIMIT = 10;

    /** Limeite de atividades recentes exibidas. */
    static final int RECENT_ACTIVITY_LIMIT = 8;

    private final OpportunityRepository opportunityRepository;
    private final StageRepository stageRepository;
    private final PipelineRepository pipelineRepository;
    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;
    private final TaskRepository taskRepository;

    public DashboardService(OpportunityRepository opportunityRepository,
                            StageRepository stageRepository,
                            PipelineRepository pipelineRepository,
                            ContactRepository contactRepository,
                            ActivityRepository activityRepository,
                            TaskRepository taskRepository) {
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.pipelineRepository = pipelineRepository;
        this.contactRepository = contactRepository;
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public OperationalDashboard build(UUID companyId) {
        var stages = stageRepository.findByCompanyId(companyId);
        Map<UUID, Stage> stageById = stages.stream().collect(Collectors.toMap(Stage::getId, Function.identity()));

        var pipelines = pipelineRepository.findByCompanyId(companyId);
        Map<UUID, Pipeline> pipelineById = pipelines.stream().collect(Collectors.toMap(Pipeline::getId, Function.identity()));

        var opportunities = opportunityRepository.findByCompanyId(companyId);
        List<Opportunity> open = opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.OPEN)
                .toList();

        var openValue = open.stream()
                .map(Opportunity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime now = LocalDateTime.now();
        List<AttentionOpportunity> attention = open.stream()
                .map(o -> buildAttention(o, stageById, pipelineById, now))
                .sorted(Comparator.comparingInt(AttentionOpportunity::priorityScore).reversed())
                .limit(ATTENTION_LIMIT)
                .toList();

        var dueToday = taskRepository.findDueToday(companyId, LocalDate.now().atStartOfDay(),
                        LocalDate.now().atTime(LocalTime.MAX))
                .stream()
                .filter(t -> t.getStatus() != com.becommerce.crm.domain.task.TaskStatus.COMPLETED)
                .map(this::toTaskResponse)
                .toList();

        var recent = activityRepository.findRecentByCompanyId(companyId, RECENT_ACTIVITY_LIMIT)
                .stream()
                .map(this::toActivityResponse)
                .toList();

        boolean isEmpty = open.isEmpty() && dueToday.isEmpty() && attention.isEmpty();
        String greeting = isEmpty
                ? "Tudo em dia. Aproveite para revisar suas oportunidades paradas."
                : "Aqui está o que merece atenção hoje.";

        return new OperationalDashboard(
                greeting,
                attention.size(),
                attention.stream().filter(AttentionOpportunity::stale).count(),
                dueToday.size(),
                open.size(),
                openValue,
                attention,
                dueToday,
                recent
        );
    }

    private AttentionOpportunity buildAttention(Opportunity o, Map<UUID, Stage> stageById,
                                                Map<UUID, Pipeline> pipelineById, LocalDateTime now) {
        Stage stage = stageById.get(o.getStageId());
        Pipeline pipeline = pipelineById.get(o.getPipelineId());

        LocalDateTime lastActivityAt = activityRepository.findLatestActivityAtByOpportunityId(o.getId()).orElse(o.getCreatedAt());
        long daysInactive = java.time.Duration.between(lastActivityAt, now).toDays();
        boolean stale = daysInactive >= STALE_DAYS;

        // Score determinístico: peso do valor + probabilidade do estágio + tempo parado.
        int probability = stage != null ? stage.getProbability() : 0;
        int valueScore = o.getValue() != null ? o.getValue().divide(BigDecimal.valueOf(1000)).intValue() : 0;
        int staleScore = (int) Math.min(daysInactive, STALE_DAYS * 3);
        int priorityScore = valueScore + probability + staleScore;

        String suggestion = suggestionFor(o, stale, daysInactive, stage);

        return new AttentionOpportunity(
                o.getId(),
                o.getTitle(),
                o.getValue(),
                contactName(o.getContactId()),
                stage != null ? stage.getName() : "—",
                stage != null ? stage.getOrderNum() : 0,
                pipeline != null ? pipeline.getName() : "—",
                stale,
                daysInactive,
                suggestion,
                priorityScore
        );
    }

    private String suggestionFor(Opportunity o, boolean stale, long daysInactive, Stage stage) {
        if (stale) {
            return "Sem contato há " + daysInactive + " dias. Agende um follow-up.";
        }
        if (o.getExpectedCloseDate() != null && o.getExpectedCloseDate().minusDays(3).isBefore(LocalDateTime.now())) {
            return "Fechamento próximo. Revise os próximos passos.";
        }
        if (stage != null && stage.getProbability() >= 70) {
            return "Alta probabilidade. Considere uma proposta formal.";
        }
        return "Sem atividade recente. Mantenha o ritmo.";
    }

    private String contactName(UUID contactId) {
        if (contactId == null) {
            return null;
        }
        return contactRepository.findById(contactId)
                .map(c -> (c.getFirstName() + " " + c.getLastName()).trim())
                .orElse(null);
    }

    private TaskResponse toTaskResponse(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getCompanyId(),
                t.getContactId(),
                t.getOpportunityId(),
                t.getTitle(),
                t.getDescription(),
                t.getAssigneeId(),
                t.getDueAt(),
                t.getPriority(),
                t.getStatus(),
                t.getCompletedAt(),
                t.getCreatedBy(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private ActivityResponse toActivityResponse(com.becommerce.crm.domain.activity.Activity a) {
        return new ActivityResponse(
                a.getId(),
                a.getCompanyId(),
                a.getContactId(),
                a.getOpportunityId(),
                a.getType(),
                a.getSubject(),
                a.getDescription(),
                a.getActivityAt(),
                a.getCreatedBy(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    @SuppressWarnings("unused")
    private UUID currentCompanyId() {
        return TenantContext.getCompanyId();
    }
}