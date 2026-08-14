package com.becommerce.crm.application.pipeline.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.pipeline.dto.CreatePipelineRequest;
import com.becommerce.crm.application.pipeline.dto.CreateStageRequest;
import com.becommerce.crm.application.pipeline.dto.PipelineMetricsResponse;
import com.becommerce.crm.application.pipeline.dto.PipelineResponse;
import com.becommerce.crm.application.pipeline.dto.ReorderStagesRequest;
import com.becommerce.crm.application.pipeline.dto.StageResponse;
import com.becommerce.crm.application.pipeline.dto.UpdatePipelineRequest;
import com.becommerce.crm.application.pipeline.dto.UpdateStageRequest;
import com.becommerce.crm.application.pipeline.port.input.PipelineUseCase;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.pipeline.exception.PipelineNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.PipelineValidationException;
import com.becommerce.crm.domain.pipeline.exception.StageNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipelines e estágios (Sprint 11). Um pipeline nasce com estágios padrão
 * (Prospecção → Qualificação → Proposta → Negociação → Fechamento) e mantém
 * entre 2 e 15 estágios ordenados (P-002/P-003/P-004). Toda operação roda sob
 * {@code TenantContext} da empresa ativa (defense-in-depth além do RLS FORCE).
 */
@Service
public class PipelineService implements PipelineUseCase {

    private static final Map<String, Integer> DEFAULT_STAGES = new LinkedHashMap<>();
    static {
        DEFAULT_STAGES.put("Prospecção", 10);
        DEFAULT_STAGES.put("Qualificação", 20);
        DEFAULT_STAGES.put("Proposta", 45);
        DEFAULT_STAGES.put("Negociação", 70);
        DEFAULT_STAGES.put("Fechamento", 95);
    }

    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final OpportunityRepository opportunityRepository;
    private final TenantAuditRecorder auditor;

    public PipelineService(PipelineRepository pipelineRepository, StageRepository stageRepository,
                           OpportunityRepository opportunityRepository, TenantAuditRecorder auditor) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.opportunityRepository = opportunityRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public PipelineResponse create(UUID companyId, CreatePipelineRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = Pipeline.create(companyId, request.name(), request.description());
            pipelineRepository.save(pipeline);

            int order = 1;
            for (Map.Entry<String, Integer> defaultStage : DEFAULT_STAGES.entrySet()) {
                stageRepository.save(Stage.create(pipeline.getId(), companyId,
                        defaultStage.getKey(), null, order++, defaultStage.getValue()));
            }

            auditor.record(companyId, AuditAction.CREATE, AuditModule.PIPELINE, "Pipeline",
                    pipeline.getId().toString(), "Pipeline criado: " + pipeline.getName(),
                    createdBy, Map.of("stages", String.valueOf(DEFAULT_STAGES.size())));

            return toResponse(pipeline, stagesOf(pipeline.getId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PipelineResponse update(UUID companyId, UUID pipelineId, UpdatePipelineRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            pipeline.update(request.name() != null ? request.name() : pipeline.getName(),
                    request.description() != null ? request.description() : pipeline.getDescription());
            pipelineRepository.save(pipeline);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Pipeline",
                    pipeline.getId().toString(), "Pipeline atualizado: " + pipeline.getName(),
                    null, Map.of("active", String.valueOf(pipeline.isActive())));

            return toResponse(pipeline, stagesOf(pipeline.getId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse getById(UUID companyId, UUID pipelineId) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            return toResponse(pipeline, stagesOf(pipeline.getId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineResponse> list(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return pipelineRepository.findByCompanyId(companyId).stream()
                    .map(p -> toResponse(p, stagesOf(p.getId())))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID pipelineId) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            pipelineRepository.delete(pipeline);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.PIPELINE, "Pipeline",
                    pipelineId.toString(), "Pipeline excluído: " + pipeline.getName(),
                    null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PipelineResponse addStage(UUID companyId, UUID pipelineId, CreateStageRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            List<Stage> stages = stagesOf(pipeline.getId());
            if (stages.size() >= Pipeline.MAX_STAGES) {
                throw new PipelineValidationException(
                        "Um pipeline pode ter no máximo " + Pipeline.MAX_STAGES + " estágios.");
            }
            int nextOrder = stages.stream().mapToInt(Stage::getOrderNum).max().orElse(0) + 1;
            stageRepository.save(Stage.create(pipeline.getId(), companyId, request.name(),
                    request.color(), nextOrder, request.probability() != null ? request.probability() : 0));

            List<Stage> updated = stagesOf(pipeline.getId());
            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Pipeline",
                    pipeline.getId().toString(), "Estágio adicionado: " + request.name(),
                    null, Map.of("stages", String.valueOf(updated.size())));

            return toResponse(pipeline, updated);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PipelineResponse updateStage(UUID companyId, UUID pipelineId, UUID stageId, UpdateStageRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            Stage stage = requireOwnedStage(companyId, stageId);
            if (!stage.getPipelineId().equals(pipelineId)) {
                throw new StageNotFoundException(stageId);
            }
            stage.update(request.name(), request.color(), request.probability());
            stageRepository.save(stage);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Stage",
                    stageId.toString(), "Estágio atualizado: " + stage.getName(),
                    null, Map.of("pipelineId", pipelineId.toString()));

            return toResponse(pipeline, stagesOf(pipeline.getId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public PipelineResponse reorderStages(UUID companyId, UUID pipelineId, ReorderStagesRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            List<Stage> existing = stagesOf(pipeline.getId());
            if (existing.size() != request.stages().size()) {
                throw new PipelineValidationException(
                        "A reordenação deve conter exatamente todos os estágios do pipeline.");
            }
            Map<UUID, Stage> byId = new LinkedHashMap<>();
            existing.forEach(s -> byId.put(s.getId(), s));
            int order = 1;
            for (ReorderStagesRequest.StagedItem item : request.stages()) {
                Stage stage = byId.get(item.id());
                if (stage == null) {
                    throw new StageNotFoundException(item.id());
                }
                stageRepository.save(Stage.reconstitute(stage.getId(), stage.getPipelineId(),
                        stage.getCompanyId(), stage.getName(), stage.getColor(), order++,
                        stage.getProbability(), stage.getCreatedAt(), LocalDateTime.now()));
            }

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Pipeline",
                    pipeline.getId().toString(), "Estágios reordenados",
                    null, Map.of("stages", String.valueOf(request.stages().size())));

            return toResponse(pipeline, stagesOf(pipeline.getId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineMetricsResponse metrics(UUID companyId, UUID pipelineId) {
        try {
            TenantContext.setCompanyId(companyId);
            Pipeline pipeline = requireOwned(companyId, pipelineId);
            List<Stage> stages = stagesOf(pipeline.getId());
            Map<UUID, Stage> stageById = new LinkedHashMap<>();
            stages.forEach(s -> stageById.put(s.getId(), s));

            List<Opportunity> opportunities = opportunityRepository.findByPipelineId(pipelineId);

            int wonCount = 0;
            int lostCount = 0;
            BigDecimal wonValue = BigDecimal.ZERO;
            BigDecimal lostValue = BigDecimal.ZERO;
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal forecast = BigDecimal.ZERO;
            long cycleSeconds = 0;
            int closedCount = 0;

            Map<UUID, Integer> stageCount = new LinkedHashMap<>();
            Map<UUID, BigDecimal> stageValue = new LinkedHashMap<>();

            for (Opportunity opp : opportunities) {
                switch (opp.getStatus()) {
                    case WON -> {
                        wonCount++;
                        wonValue = wonValue.add(opp.getValue());
                        closedCount++;
                        cycleSeconds += Duration.between(opp.getCreatedAt(), opp.getWonAt()).getSeconds();
                    }
                    case LOST -> {
                        lostCount++;
                        lostValue = lostValue.add(opp.getValue());
                        closedCount++;
                        cycleSeconds += Duration.between(opp.getCreatedAt(), opp.getLostAt()).getSeconds();
                    }
                    default -> {
                        totalValue = totalValue.add(opp.getValue());
                        Stage s = stageById.get(opp.getStageId());
                        double prob = (s != null ? s.getProbability() : 0) / 100.0;
                        forecast = forecast.add(opp.getValue().multiply(BigDecimal.valueOf(prob), MathContext.DECIMAL64));
                        stageCount.merge(opp.getStageId(), 1, Integer::sum);
                        stageValue.merge(opp.getStageId(), opp.getValue(), BigDecimal::add);
                    }
                }
            }

            BigDecimal winRate = (wonCount + lostCount) > 0
                    ? BigDecimal.valueOf(wonCount).divide(BigDecimal.valueOf(wonCount + lostCount), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Double avgCycleDays = closedCount > 0
                    ? (cycleSeconds / 86400.0) / closedCount
                    : null;

            List<PipelineMetricsResponse.StageMetric> byStage = stages.stream()
                    .map(s -> new PipelineMetricsResponse.StageMetric(
                            s.getId(), s.getName(),
                            stageCount.getOrDefault(s.getId(), 0),
                            stageValue.getOrDefault(s.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)))
                    .toList();

            return new PipelineMetricsResponse(pipelineId, stageCount.values().stream().mapToInt(Integer::intValue).sum(),
                    wonCount, lostCount,
                    totalValue.setScale(2, RoundingMode.HALF_UP),
                    wonValue.setScale(2, RoundingMode.HALF_UP),
                    lostValue.setScale(2, RoundingMode.HALF_UP),
                    winRate, avgCycleDays,
                    forecast.setScale(2, RoundingMode.HALF_UP), byStage);
        } finally {
            TenantContext.clear();
        }
    }

    private Pipeline requireOwned(UUID companyId, UUID pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new PipelineNotFoundException(pipelineId));
        if (!pipeline.getCompanyId().equals(companyId)) {
            throw new PipelineNotFoundException(pipelineId);
        }
        return pipeline;
    }

    private Stage requireOwnedStage(UUID companyId, UUID stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new StageNotFoundException(stageId));
        if (!stage.getCompanyId().equals(companyId)) {
            throw new StageNotFoundException(stageId);
        }
        return stage;
    }

    private List<Stage> stagesOf(UUID pipelineId) {
        return stageRepository.findByPipelineIdOrdered(pipelineId);
    }

    private static PipelineResponse toResponse(Pipeline p, List<Stage> stages) {
        List<StageResponse> stageResponses = stages.stream()
                .map(s -> new StageResponse(s.getId(), s.getPipelineId(), s.getName(), s.getColor(),
                        s.getOrderNum(), s.getProbability(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
        return new PipelineResponse(p.getId(), p.getCompanyId(), p.getName(), p.getDescription(),
                p.isActive(), stageResponses, p.getCreatedAt(), p.getUpdatedAt());
    }
}
