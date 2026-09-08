package com.becommerce.crm.application.followup.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceRequest;
import com.becommerce.crm.application.followup.dto.FollowUpSequenceResponse;
import com.becommerce.crm.application.followup.port.input.FollowUpSequenceUseCase;
import com.becommerce.crm.application.followup.port.output.FollowUpSequenceRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.followup.FollowUpSequence;
import com.becommerce.crm.domain.followup.exception.FollowUpSequenceNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Casos de uso de {@code FollowUpSequence} (Sprint 22). O tenant vem do usuário
 * autenticado (CurrentUser) — nunca do client — e é aplicado via
 * {@link TenantContext} + RLS FORCE. Auditoria reutiliza
 * {@link TenantAuditRecorder}.
 */
@Service
public class FollowUpSequenceService implements FollowUpSequenceUseCase {

    private static final Logger log = LoggerFactory.getLogger(FollowUpSequenceService.class);

    private final FollowUpSequenceRepository sequenceRepository;
    private final TenantAuditRecorder auditor;

    public FollowUpSequenceService(FollowUpSequenceRepository sequenceRepository,
                                   TenantAuditRecorder auditor) {
        this.sequenceRepository = sequenceRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public FollowUpSequenceResponse create(UUID companyId, FollowUpSequenceRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            FollowUpSequence sequence = FollowUpSequence.create(companyId, request.name(), request.description());
            FollowUpSequence saved = sequenceRepository.save(sequence);
            audit(companyId, saved.getId(), AuditAction.CREATE, "Sequência de follow-up criada");
            return FollowUpSequenceResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUpSequenceResponse get(UUID companyId, UUID sequenceId) {
        try {
            TenantContext.setCompanyId(companyId);
            return FollowUpSequenceResponse.from(requireOwned(companyId, sequenceId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowUpSequenceResponse> list(UUID companyId, int page, int pageSize) {
        try {
            TenantContext.setCompanyId(companyId);
            PageResponse<FollowUpSequence> result = sequenceRepository.findByCompany(companyId, page, pageSize);
            return PageResponse.of(
                    result.content().stream().map(FollowUpSequenceResponse::from).toList(),
                    page, pageSize, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public FollowUpSequenceResponse update(UUID companyId, UUID sequenceId, FollowUpSequenceRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            FollowUpSequence sequence = requireOwned(companyId, sequenceId);
            sequence.update(request.name(), request.description());
            FollowUpSequence saved = sequenceRepository.save(sequence);
            audit(companyId, sequenceId, AuditAction.UPDATE, "Sequência de follow-up atualizada");
            return FollowUpSequenceResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID sequenceId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwned(companyId, sequenceId);
            sequenceRepository.delete(companyId, sequenceId);
            audit(companyId, sequenceId, AuditAction.DELETE, "Sequência de follow-up excluída");
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public FollowUpSequenceResponse activate(UUID companyId, UUID sequenceId) {
        return changeStatus(companyId, sequenceId, true, "Sequência de follow-up ativada");
    }

    @Override
    @Transactional
    public FollowUpSequenceResponse deactivate(UUID companyId, UUID sequenceId) {
        return changeStatus(companyId, sequenceId, false, "Sequência de follow-up inativada");
    }

    private FollowUpSequenceResponse changeStatus(UUID companyId, UUID sequenceId,
                                                   boolean activate, String auditDescription) {
        try {
            TenantContext.setCompanyId(companyId);
            FollowUpSequence sequence = requireOwned(companyId, sequenceId);
            if (activate) {
                sequence.activate();
            } else {
                sequence.deactivate();
            }
            FollowUpSequence saved = sequenceRepository.save(sequence);
            audit(companyId, sequenceId, AuditAction.UPDATE, auditDescription);
            return FollowUpSequenceResponse.from(saved);
        } finally {
            TenantContext.clear();
        }
    }

    private FollowUpSequence requireOwned(UUID companyId, UUID sequenceId) {
        FollowUpSequence sequence = sequenceRepository.findById(sequenceId)
                .orElseThrow(() -> new FollowUpSequenceNotFoundException(sequenceId));
        if (!sequence.getCompanyId().equals(companyId)) {
            throw new FollowUpSequenceNotFoundException(sequenceId);
        }
        return sequence;
    }

    private void audit(UUID companyId, UUID sequenceId, AuditAction action, String description) {
        try {
            auditor.record(companyId, action, AuditModule.FOLLOWUPS, "FollowUpSequence",
                    sequenceId.toString(), description, null, null);
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar auditoria de FollowUpSequence (company={}, sequence={}): {}",
                    companyId, sequenceId, e.getMessage());
        }
    }
}