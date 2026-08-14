package com.becommerce.crm.application.pipeline.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.pipeline.dto.CreateOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.MarkLostRequest;
import com.becommerce.crm.application.pipeline.dto.MoveDirection;
import com.becommerce.crm.application.pipeline.dto.MoveOpportunityRequest;
import com.becommerce.crm.application.pipeline.dto.OpportunityHistoryResponse;
import com.becommerce.crm.application.pipeline.dto.OpportunityResponse;
import com.becommerce.crm.application.pipeline.dto.UpdateOpportunityRequest;
import com.becommerce.crm.application.pipeline.port.input.OpportunityUseCase;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.PipelineRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityHistory;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Pipeline;
import com.becommerce.crm.domain.pipeline.Stage;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.PipelineNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.PipelineValidationException;
import com.becommerce.crm.domain.pipeline.exception.StageNotFoundException;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Oportunidades (Sprint 11). Regras: contato da própria empresa e ativo
 * (P-010/P-011); movimento de ±1 estágio (P-020); ganha/perdida apenas no
 * último estágio (P-021); motivo obrigatório em perda (P-022); histórico
 * imutável por movimentação (P-024). Tudo isolado pela empresa ativa.
 */
@Service
public class OpportunityService implements OpportunityUseCase {

    private final OpportunityRepository opportunityRepository;
    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final ContactRepository contactRepository;
    private final TenantAuditRecorder auditor;
    private final EventPublisher eventPublisher;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              PipelineRepository pipelineRepository,
                              StageRepository stageRepository,
                              ContactRepository contactRepository,
                              TenantAuditRecorder auditor,
                              EventPublisher eventPublisher) {
        this.opportunityRepository = opportunityRepository;
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.contactRepository = contactRepository;
        this.auditor = auditor;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OpportunityResponse create(UUID companyId, UUID pipelineId, CreateOpportunityRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedPipeline(companyId, pipelineId);
            requireOwnedActiveContact(companyId, request.contactId());
            List<Stage> stages = orderedStages(pipelineId);
            if (stages.isEmpty()) {
                throw new PipelineValidationException("Pipeline sem estágios não pode receber oportunidades.");
            }
            Stage first = stages.stream().min(Comparator.comparingInt(Stage::getOrderNum))
                    .orElseThrow(() -> new StageNotFoundException(pipelineId));

            Opportunity opportunity = Opportunity.create(companyId, request.title(), request.value(),
                    request.contactId(), pipelineId, first.getId(), request.assignedTo(),
                    request.expectedCloseDate(), request.notes());
            opportunityRepository.save(opportunity);

            recordHistory(opportunity.getId(), null, first.getId(), createdBy,
                    "Oportunidade criada no estágio " + first.getName());
            auditor.record(companyId, AuditAction.CREATE, AuditModule.PIPELINE, "Opportunity",
                    opportunity.getId().toString(), "Oportunidade criada: " + opportunity.getTitle(),
                    createdBy, Map.of("pipelineId", pipelineId.toString()));
            eventPublisher.publish(WorkflowTriggerEvent.opportunityCreated(companyId, opportunity.getId(),
                    opportunity.getContactId(), first.getName(), opportunity.getValue()));

            return toResponse(opportunity, first);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityResponse getById(UUID companyId, UUID opportunityId) {
        try {
            TenantContext.setCompanyId(companyId);
            Opportunity opportunity = requireOwned(companyId, opportunityId);
            return toResponse(opportunity, stageOf(companyId, opportunity.getStageId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public OpportunityResponse update(UUID companyId, UUID opportunityId, UpdateOpportunityRequest request, UUID changedBy) {
        try {
            TenantContext.setCompanyId(companyId);
            Opportunity opportunity = requireOwned(companyId, opportunityId);
            opportunity.update(request.title(), request.value(), request.assignedTo(),
                    request.expectedCloseDate(), request.notes());
            opportunityRepository.save(opportunity);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Opportunity",
                    opportunity.getId().toString(), "Oportunidade atualizada: " + opportunity.getTitle(),
                    changedBy, null);

            return toResponse(opportunity, stageOf(companyId, opportunity.getStageId()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public OpportunityResponse move(UUID companyId, UUID opportunityId, MoveOpportunityRequest request, UUID changedBy) {
        try {
            TenantContext.setCompanyId(companyId);
            Opportunity opportunity = requireOwned(companyId, opportunityId);
            List<Stage> stages = orderedStages(opportunity.getPipelineId());
            int currentIndex = indexOf(stages, opportunity.getStageId());

            int targetIndex;
            if (request.direction() == MoveDirection.ADVANCE) {
                if (currentIndex >= stages.size() - 1) {
                    throw new PipelineValidationException(
                            "Oportunidade já está no último estágio; conclua como ganha ou perdida.");
                }
                targetIndex = currentIndex + 1;
            } else {
                if (currentIndex <= 0) {
                    throw new PipelineValidationException("Oportunidade já está no primeiro estágio.");
                }
                targetIndex = currentIndex - 1;
            }

            Stage current = stages.get(currentIndex);
            Stage target = stages.get(targetIndex);
            UUID from = opportunity.getStageId();
            opportunity.moveTo(target.getId());
            opportunityRepository.save(opportunity);
            recordHistory(opportunity.getId(), from, target.getId(), changedBy,
                    request.note() != null ? request.note()
                            : (request.direction() == MoveDirection.ADVANCE ? "Avançou" : "Regrediu")
                    + " para " + target.getName());

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Opportunity",
                    opportunity.getId().toString(),
                    "Oportunidade movida: " + current.getName() + " → " + target.getName(),
                    changedBy, null);
            eventPublisher.publish(WorkflowTriggerEvent.opportunityStageChanged(companyId, opportunity.getId(),
                    opportunity.getContactId(), target.getName(), opportunity.getValue()));

            return toResponse(opportunity, target);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public OpportunityResponse markWon(UUID companyId, UUID opportunityId, UUID changedBy) {
        try {
            TenantContext.setCompanyId(companyId);
            Opportunity opportunity = requireOwned(companyId, opportunityId);
            requireLastStage(opportunity);
            Stage current = stageOf(companyId, opportunity.getStageId());
            opportunity.markWon(LocalDateTime.now());
            opportunityRepository.save(opportunity);
            recordHistory(opportunity.getId(), current.getId(), current.getId(), changedBy,
                    "Oportunidade ganha (" + opportunity.getValue() + ")");

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Opportunity",
                    opportunity.getId().toString(), "Oportunidade ganha: " + opportunity.getTitle(),
                    changedBy, Map.of("value", opportunity.getValue().toPlainString()));
            eventPublisher.publish(WorkflowTriggerEvent.opportunityWon(companyId, opportunity.getId(),
                    opportunity.getContactId(), current.getName(), opportunity.getValue()));

            return toResponse(opportunity, current);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public OpportunityResponse markLost(UUID companyId, UUID opportunityId, MarkLostRequest request, UUID changedBy) {
        try {
            TenantContext.setCompanyId(companyId);
            if (request.lossReason() == null || request.lossReason().isBlank()) {
                throw new PipelineValidationException("O motivo da perda é obrigatório.");
            }
            Opportunity opportunity = requireOwned(companyId, opportunityId);
            requireLastStage(opportunity);
            Stage current = stageOf(companyId, opportunity.getStageId());
            opportunity.markLost(request.lossReason().trim(), LocalDateTime.now());
            opportunityRepository.save(opportunity);
            recordHistory(opportunity.getId(), current.getId(), current.getId(), changedBy,
                    "Oportunidade perdida: " + request.lossReason());

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PIPELINE, "Opportunity",
                    opportunity.getId().toString(), "Oportunidade perdida: " + opportunity.getTitle(),
                    changedBy, Map.of("lossReason", request.lossReason()));
            eventPublisher.publish(WorkflowTriggerEvent.opportunityLost(companyId, opportunity.getId(),
                    opportunity.getContactId(), current.getName(), opportunity.getValue()));

            return toResponse(opportunity, current);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID opportunityId) {
        try {
            TenantContext.setCompanyId(companyId);
            Opportunity opportunity = requireOwned(companyId, opportunityId);
            opportunityRepository.delete(opportunity);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.PIPELINE, "Opportunity",
                    opportunityId.toString(), "Oportunidade excluída: " + opportunity.getTitle(),
                    null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpportunityResponse> listByPipeline(UUID companyId, UUID pipelineId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedPipeline(companyId, pipelineId);
            Map<UUID, Stage> stageById = new java.util.HashMap<>();
            orderedStages(pipelineId).forEach(s -> stageById.put(s.getId(), s));
            return opportunityRepository.findByPipelineId(pipelineId).stream()
                    .map(o -> toResponse(o, stageById.get(o.getStageId())))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpportunityHistoryResponse> history(UUID companyId, UUID opportunityId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, opportunityId);
            return opportunityRepository.findHistoryByOpportunityId(opportunityId).stream()
                    .map(h -> new OpportunityHistoryResponse(h.getId(), h.getOpportunityId(),
                            h.getFromStageId(), h.getToStageId(), h.getChangedBy(),
                            h.getChangedAt(), h.getNote()))
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    private void requireOwnedPipeline(UUID companyId, UUID pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new PipelineNotFoundException(pipelineId));
        if (!pipeline.getCompanyId().equals(companyId)) {
            throw new PipelineNotFoundException(pipelineId);
        }
    }

    private void requireOwnedActiveContact(UUID companyId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private Opportunity requireOwned(UUID companyId, UUID opportunityId) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
        if (!opportunity.getCompanyId().equals(companyId)) {
            throw new OpportunityNotFoundException(opportunityId);
        }
        return opportunity;
    }

    private void requireLastStage(Opportunity opportunity) {
        List<Stage> stages = orderedStages(opportunity.getPipelineId());
        Stage last = stages.stream().max(Comparator.comparingInt(Stage::getOrderNum))
                .orElseThrow(() -> new PipelineValidationException("Pipeline sem estágios."));
        if (!opportunity.getStageId().equals(last.getId())) {
            throw new PipelineValidationException(
                    "Oportunidade só pode ser concluída (ganha/perdida) no último estágio.");
        }
    }

    private List<Stage> orderedStages(UUID pipelineId) {
        return stageRepository.findByPipelineIdOrdered(pipelineId);
    }

    private int indexOf(List<Stage> stages, UUID stageId) {
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).getId().equals(stageId)) {
                return i;
            }
        }
        throw new PipelineValidationException("Estágio da oportunidade não pertence ao pipeline.");
    }

    private Stage stageOf(UUID companyId, UUID stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new StageNotFoundException(stageId));
        if (!stage.getCompanyId().equals(companyId)) {
            throw new StageNotFoundException(stageId);
        }
        return stage;
    }

    private void recordHistory(UUID opportunityId, UUID fromStageId, UUID toStageId,
                               UUID changedBy, String note) {
        opportunityRepository.saveHistory(
                OpportunityHistory.create(opportunityId, fromStageId, toStageId, changedBy, note));
    }

    private static OpportunityResponse toResponse(Opportunity o, Stage stage) {
        int probability = stage != null ? stage.getProbability() : 0;
        String stageName = stage != null ? stage.getName() : null;
        return new OpportunityResponse(o.getId(), o.getCompanyId(), o.getTitle(), o.getValue(),
                o.getContactId(), o.getPipelineId(), o.getStageId(), stageName, probability,
                o.getAssignedTo(), o.getExpectedCloseDate(), o.getStatus(), o.getWonAt(), o.getLostAt(),
                o.getLossReason(), o.getNotes(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
