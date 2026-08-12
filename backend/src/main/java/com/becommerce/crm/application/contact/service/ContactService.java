package com.becommerce.crm.application.contact.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.company.service.CompanyQuotaService;
import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ContactService(ContactRepository contactRepository,
                          CompanyQuotaService quotaService,
                          TenantAuditRecorder auditor) {
        this.contactRepository = contactRepository;
        this.quotaService = quotaService;
        this.auditor = auditor;
    }

    @Override
    @Transactional
    public ContactResponse create(UUID companyId, CreateContactRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            quotaService.assertCanAddContact(companyId);

            Contact contact = Contact.create(
                    companyId, request.firstName(), request.lastName(),
                    request.email(), request.phone(), request.notes());
            Contact saved = contactRepository.save(contact);

            auditor.record(companyId, AuditAction.CREATE, AuditModule.CONTACTS, "Contact",
                    saved.getId().toString(),
                    "Contato criado: " + trim(firstName(saved)),
                    createdBy, Map.of("email", String.valueOf(contact.getEmail())));
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
            Contact contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> new com.becommerce.crm.domain.contact.exception.ContactNotFoundException(contactId));
            if (!contact.getCompanyId().equals(companyId)) {
                throw new com.becommerce.crm.domain.contact.exception.ContactNotFoundException(contactId);
            }
            return toResponse(contact);
        } finally {
            TenantContext.clear();
        }
    }

    private static ContactResponse toResponse(Contact c) {
        return new ContactResponse(
                c.getId(), c.getCompanyId(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getPhone(), c.getNotes(), c.getCreatedAt());
    }

    private String firstName(Contact c) {
        return (c.getFirstName() != null ? c.getFirstName() : "")
                + (c.getLastName() != null ? " " + c.getLastName() : "");
    }

    private String trim(String value) {
        String v = value == null ? "" : value.trim();
        return v.length() > 60 ? v.substring(0, 60) : v;
    }
}