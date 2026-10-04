package com.becommerce.crm.masterdata.contact.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.service.CompanyQuotaService;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contatos (Sprint 8.6). A criação é bloqueada quando a empresa atinge
 * {@code max_contacts} (enforcement via {@link CompanyQuotaService}).
 */
@Service
public class ContactService implements ContactUseCase {

    private final ContactRepository contactRepository;
    private final CompanyQuotaService quotaService;
    private final TenantAuditRecorder auditor;
    private final com.becommerce.crm.identity.application.port.output.EventPublisher eventPublisher;
    private final com.becommerce.crm.shared.security.authorization.CurrentUserAuthorities authorities;

    public ContactService(ContactRepository contactRepository,
                          CompanyQuotaService quotaService,
                          TenantAuditRecorder auditor,
                          com.becommerce.crm.identity.application.port.output.EventPublisher eventPublisher,
                          com.becommerce.crm.shared.security.authorization.CurrentUserAuthorities authorities) {
        this.contactRepository = contactRepository;
        this.quotaService = quotaService;
        this.auditor = auditor;
        this.eventPublisher = eventPublisher;
        this.authorities = authorities;
    }

    @Override
    @Transactional
    public ContactResponse create(UUID companyId, CreateContactRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            quotaService.assertCanAddContact(companyId);

            Contact contact = Contact.create(
                    companyId, request.firstName(), request.lastName(),
                    request.email(), request.phone(), request.mobile(), request.notes(),
                    request.birthDate(), request.cpf(), request.rg(), request.rgIssuer(),
                    request.gender(), request.maritalStatus(), request.professionalStatus());
            Contact saved = contactRepository.save(contact);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, "Contact",
                    saved.getId().toString(),
                    "Contato criado: " + trim(firstName(saved)),
                    createdBy, Map.of("email", String.valueOf(contact.getEmail())));

            // Sprint 18: dispara automações (workflows) de contato criado
            eventPublisher.publish(com.becommerce.crm.automation.workflow.domain.event.WorkflowTriggerEvent
                    .contactCreated(companyId, saved.getId(), saved.getEmail(), saved.getPhone()));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ContactResponse getById(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            Contact contact = requireOwnedActive(companyId, contactId);
            return toResponse(contact);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return contactRepository.findByCompanyIdActive(companyId).stream()
                    .map(ContactService::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> search(UUID companyId, String query, int limit) {
        try {
            TenantContext.setCompanyId(companyId);
            String q = query == null ? "" : query.trim().toLowerCase();
            return contactRepository.findByCompanyIdActive(companyId).stream()
                    .filter(c -> q.isEmpty() || matches(c, q))
                    .limit(Math.max(1, limit))
                    .map(ContactService::toResponse)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    private void requireFieldPermission(String currentValue, String newValue,
                                        String permission, String fieldLabel) {
        String normalizedNew = blankToNull(newValue);
        String normalizedCurrent = blankToNull(currentValue);
        if (normalizedNew == null || java.util.Objects.equals(normalizedNew, normalizedCurrent)) {
            return;
        }
        if (!authorities.has(permission)) {
            throw new com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException(
                    "Você não tem permissão para alterar o campo " + fieldLabel + " do contato.");
        }
    }

    private static boolean matches(Contact c, String q) {
        return (c.getFirstName() != null && c.getFirstName().toLowerCase().contains(q))
                || (c.getLastName() != null && c.getLastName().toLowerCase().contains(q))
                || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                || (c.getPhone() != null && c.getPhone().toLowerCase().contains(q));
    }

    @Override
    @Transactional
    public ContactResponse update(UUID companyId, UUID contactId, UpdateContactRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Contact contact = requireOwnedActive(companyId, contactId);

            // Sprint 20 — autorização granular por campo (piloto Contatos):
            // alteração de e-mail/telefone exige a permissão específica do campo.
            requireFieldPermission(contact.getEmail(), request.email(),
                    "contact:field:email:update", "e-mail");
            requireFieldPermission(contact.getPhone(), request.phone(),
                    "contact:field:phone:update", "telefone");

            if (request.firstName() != null) contact.setFirstName(request.firstName());
            if (request.lastName() != null) contact.setLastName(blankToNull(request.lastName()));
            if (request.email() != null) contact.setEmail(blankToNull(request.email()));
            if (request.phone() != null) contact.setPhone(blankToNull(request.phone()));
            if (request.mobile() != null) contact.setMobile(blankToNull(request.mobile()));
            if (request.notes() != null) contact.setNotes(blankToNull(request.notes()));
            if (request.birthDate() != null) contact.setBirthDate(request.birthDate());
            if (request.cpf() != null) contact.setCpf(blankToNull(request.cpf()));
            if (request.rg() != null) contact.setRg(blankToNull(request.rg()));
            if (request.rgIssuer() != null) contact.setRgIssuer(blankToNull(request.rgIssuer()));
            if (request.gender() != null) contact.setGender(blankToNull(request.gender()));
            if (request.maritalStatus() != null) contact.setMaritalStatus(blankToNull(request.maritalStatus()));
            if (request.professionalStatus() != null) contact.setProfessionalStatus(blankToNull(request.professionalStatus()));
            contact.touch();
            Contact saved = contactRepository.save(contact);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.CONTACTS, "Contact",
                    saved.getId().toString(), "Contato atualizado: " + trim(firstName(saved)),
                    null, Map.of("email", String.valueOf(saved.getEmail())));
            return toResponse(saved);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID contactId) {
        try {
            TenantContext.setCompanyId(companyId);
            Contact contact = requireOwnedActive(companyId, contactId);
            contact.delete();
            Contact saved = contactRepository.save(contact);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.CONTACTS, "Contact",
                    saved.getId().toString(), "Contato excluído: " + trim(firstName(saved)),
                    null, Map.of("email", String.valueOf(saved.getEmail())));
        } finally {
            TenantContext.clear();
        }
    }

    private Contact requireOwnedActive(UUID companyId, UUID contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));
        if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
            throw new ContactNotFoundException(contactId);
        }
        return contact;
    }

    private static ContactResponse toResponse(Contact c) {
        return new ContactResponse(
                c.getId(), c.getCompanyId(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getPhone(), c.getMobile(), c.getNotes(),
                c.getBirthDate(), c.getCpf(), c.getRg(), c.getRgIssuer(),
                c.getGender(), c.getMaritalStatus(), c.getProfessionalStatus(),
                c.getCreatedAt());
    }

    private String firstName(Contact c) {
        return (c.getFirstName() != null ? c.getFirstName() : "")
                + (c.getLastName() != null ? " " + c.getLastName() : "");
    }

    private String trim(String value) {
        String v = value == null ? "" : value.trim();
        return v.length() > 60 ? v.substring(0, 60) : v;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}