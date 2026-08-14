package com.becommerce.crm.application.activity.service;

import com.becommerce.crm.application.activity.dto.ActivityResponse;
import com.becommerce.crm.application.activity.dto.CreateActivityRequest;
import com.becommerce.crm.application.activity.dto.UpdateActivityRequest;
import com.becommerce.crm.application.activity.port.input.ActivityUseCase;
import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.activity.exception.ActivityNotFoundException;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import com.becommerce.crm.domain.workflow.event.WorkflowTriggerEvent;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Activities (Sprint 12). Cada operação isola a empresa ativa no
 * {@link TenantContext} (finally {@code clear()}); os vínculos contact/opportunity
 * são validados como pertencentes à MESMA empresa (defense-in-depth além do RLS).
 * Auditoria via {@link TenantAuditRecorder} ({@code AuditModule.ACTIVITIES}).
 */
@Service
public class ActivityService implements ActivityUseCase {

    private final ActivityRepository activityRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final TenantAuditRecorder auditor;
    private final EventPublisher eventPublisher;

    public ActivityService(ActivityRepository activityRepository,
                           ContactRepository contactRepository,
                           OpportunityRepository opportunityRepository,
                           TenantAuditRecorder auditor,
                           EventPublisher eventPublisher) {
        this.activityRepository = activityRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.auditor = auditor;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ActivityResponse create(UUID companyId, CreateActivityRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            validateOwnedLinks(companyId, request.contactId(), request.opportunityId());
            Activity activity = Activity.create(companyId, request.contactId(), request.opportunityId(),
                    request.type(), request.subject(), request.description(), request.activityAt(), createdBy);
            activityRepository.save(activity);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.ACTIVITIES, "Activity",
                    activity.getId().toString(), "Atividade registrada: " + activity.getSubject(),
                    createdBy, null);
            eventPublisher.publish(WorkflowTriggerEvent.activityCreated(companyId, activity.getId(),
                    activity.getContactId(), activity.getOpportunityId(),
                    activity.getType() != null ? activity.getType().name() : null));
            return toResponse(activity);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getById(UUID companyId, UUID activityId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, activityId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public ActivityResponse update(UUID companyId, UUID activityId, UpdateActivityRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Activity activity = requireOwned(companyId, activityId);
            activity.update(request.type(), request.subject(), request.description(), request.activityAt());
            activityRepository.save(activity);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.ACTIVITIES, "Activity",
                    activity.getId().toString(), "Atividade atualizada: " + activity.getSubject(),
                    null, null);
            return toResponse(activity);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID activityId) {
        try {
            TenantContext.setCompanyId(companyId);
            Activity activity = requireOwned(companyId, activityId);
            activityRepository.delete(activity);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.ACTIVITIES, "Activity",
                    activityId.toString(), "Atividade excluída: " + activity.getSubject(), null, null);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return activityRepository.findByCompanyId(companyId).stream()
                    .map(ActivityService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> listByContact(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedContact(companyId, contactId);
            return activityRepository.findByContactId(contactId).stream()
                    .map(ActivityService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> listByOpportunity(UUID companyId, UUID opportunityId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireOwnedOpportunity(companyId, opportunityId);
            return activityRepository.findByOpportunityId(opportunityId).stream()
                    .map(ActivityService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> recent(UUID companyId, int limit) {
        try {
            TenantContext.setCompanyId(companyId);
            return activityRepository.findRecentByCompanyId(companyId, limit).stream()
                    .map(ActivityService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    private void validateOwnedLinks(UUID companyId, UUID contactId, UUID opportunityId) {
        if (contactId != null) {
            requireOwnedContact(companyId, contactId);
        }
        if (opportunityId != null) {
            requireOwnedOpportunity(companyId, opportunityId);
        }
    }

    private void requireOwnedContact(UUID companyId, UUID contactId) {
        var contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
            throw new ContactNotFoundException(contactId);
        }
    }

    private void requireOwnedOpportunity(UUID companyId, UUID opportunityId) {
        var opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
        if (!opportunity.getCompanyId().equals(companyId)) {
            throw new OpportunityNotFoundException(opportunityId);
        }
    }

    private Activity requireOwned(UUID companyId, UUID activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));
        if (!activity.getCompanyId().equals(companyId)) {
            throw new ActivityNotFoundException(activityId);
        }
        return activity;
    }

    private static ActivityResponse toResponse(Activity a) {
        return new ActivityResponse(a.getId(), a.getCompanyId(), a.getContactId(), a.getOpportunityId(),
                a.getType(), a.getSubject(), a.getDescription(), a.getActivityAt(),
                a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }
}