package com.becommerce.crm.application.lead.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.dto.PageResponse;
import com.becommerce.crm.application.lead.dto.CreateLeadRequest;
import com.becommerce.crm.application.lead.dto.LeadResponse;
import com.becommerce.crm.application.lead.dto.UpdateLeadRequest;
import com.becommerce.crm.application.lead.port.input.LeadUseCase;
import com.becommerce.crm.application.lead.port.output.LeadRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.lead.Lead;
import com.becommerce.crm.domain.lead.LeadClassification;
import com.becommerce.crm.domain.lead.LeadSource;
import com.becommerce.crm.domain.lead.LeadStatus;
import com.becommerce.crm.domain.lead.exception.DuplicateLeadException;
import com.becommerce.crm.domain.lead.exception.LeadNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Leads (Sprint 10). Valida que o contato associado pertence e está ativo na
 * mesma empresa (defense-in-depth além do RLS FORCE), garante unicidade por
 * {@code (contact_id, company_id)} e isola todas as operações pelo
 * {@code TenantContext} da empresa ativa.
 */
@Service
public class LeadService implements LeadUseCase {

    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final TenantAuditRecorder auditor;

    public LeadService(LeadRepository leadRepository, ContactRepository contactRepository,
                       TenantAuditRecorder auditor) {
        this.leadRepository = leadRepository;
        this.contactRepository = contactRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public LeadResponse create(UUID companyId, CreateLeadRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedActiveContact(companyId, request.contactId());
            if (leadRepository.existsByContactIdAndCompanyId(request.contactId(), companyId)) {
                throw new DuplicateLeadException(request.contactId(), companyId);
            }

            Lead lead = Lead.create(
                    companyId, request.contactId(),
                    request.source() != null ? request.source() : LeadSource.MANUAL,
                    request.status() != null ? request.status() : LeadStatus.NEW,
                    request.score() != null ? request.score() : 0,
                    request.classification(), request.campaignId(), request.assignedTo(), request.notes());
            Lead saved = leadRepository.save(lead);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.LEADS, "Lead",
                    saved.getId().toString(),
                    "Lead criado: " + saved.getSource() + " -> " + saved.getStatus(),
                    createdBy, Map.of("contactId", saved.getContactId().toString()));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getById(UUID companyId, UUID leadId) {
        try {
            TenantContext.setCompanyId(companyId);
            Lead lead = requireOwned(companyId, leadId);
            return toResponse(lead);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public LeadResponse update(UUID companyId, UUID leadId, UpdateLeadRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Lead lead = requireOwned(companyId, leadId);
            if (request.status() != null) lead.transitionTo(request.status());
            if (request.score() != null) lead.updateScore(request.score());
            if (request.classification() != null) lead.updateClassification(request.classification());
            if (request.campaignId() != null) lead.setCampaignId(request.campaignId());
            if (request.assignedTo() != null) lead.assignTo(request.assignedTo());
            if (request.notes() != null) lead.setNotes(request.notes());
            lead.touch();
            Lead saved = leadRepository.save(lead);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.LEADS, "Lead",
                    saved.getId().toString(), "Lead atualizado: status=" + saved.getStatus() +
                            ", score=" + saved.getScore(),
                    null, Map.of("status", String.valueOf(saved.getStatus())));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID leadId) {
        try {
            TenantContext.setCompanyId(companyId);
            Lead lead = requireOwned(companyId, leadId);
            leadRepository.delete(lead);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.LEADS, "Lead",
                    leadId.toString(), "Lead excluído", null,
                    Map.of("status", String.valueOf(lead.getStatus())));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(UUID companyId, String status, String source, String classification,
                                           int page, int pageSize, String sortBy, String sortDirection) {
        try {
            TenantContext.setCompanyId(companyId);
            LeadRepository.PageResult result = leadRepository.findByCompanyWithFilters(
                    companyId, normalize(status), normalize(source), normalize(classification),
                    page, pageSize, sortBy, sortDirection);
            var leads = result.content().stream().map(LeadService::toResponse).toList();
            return PageResponse.of(leads, page, pageSize, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    private void requireOwnedActiveContact(UUID companyId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private Lead requireOwned(UUID companyId, UUID leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new LeadNotFoundException(leadId));
        if (!lead.getCompanyId().equals(companyId)) {
            throw new LeadNotFoundException(leadId);
        }
        return lead;
    }

    private String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return LeadStatus.valueOf(raw).name();
        } catch (IllegalArgumentException ignored) { }
        try {
            return LeadSource.valueOf(raw).name();
        } catch (IllegalArgumentException ignored) { }
        try {
            return LeadClassification.valueOf(raw).name();
        } catch (IllegalArgumentException ignored) { }
        return raw;
    }

    private static LeadResponse toResponse(Lead l) {
        return new LeadResponse(
                l.getId(), l.getCompanyId(), l.getContactId(), l.getStatus(), l.getScore(),
                l.getClassification(), l.getSource(), l.getCampaignId(), l.getAssignedTo(),
                l.getNotes(), l.getCreatedAt(), l.getUpdatedAt());
    }
}