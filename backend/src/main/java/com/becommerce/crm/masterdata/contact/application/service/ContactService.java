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
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.company.application.port.output.CompanySettingsRepository;
import com.becommerce.crm.masterdata.company.domain.CompanySettings;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
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
    private final CompanySettingsRepository companySettingsRepository;

    public ContactService(ContactRepository contactRepository,
                          CompanyQuotaService quotaService,
                          TenantAuditRecorder auditor,
                          com.becommerce.crm.identity.application.port.output.EventPublisher eventPublisher,
                          com.becommerce.crm.shared.security.authorization.CurrentUserAuthorities authorities,
                          CompanySettingsRepository companySettingsRepository) {
        this.contactRepository = contactRepository;
        this.quotaService = quotaService;
        this.auditor = auditor;
        this.eventPublisher = eventPublisher;
        this.authorities = authorities;
        this.companySettingsRepository = companySettingsRepository;
    }

    @Override
    @Transactional
    public ContactResponse create(UUID companyId, CreateContactRequest request, UUID createdBy) {
        try {
            TenantContext.setCompanyId(companyId);
            quotaService.assertCanAddContact(companyId);
            requireCpfIfConfigured(companyId, request.cpf());

            Contact contact = Contact.create(
                    companyId, request.firstName(), request.lastName(),
                    request.email(), request.phone(), request.mobile(), request.notes(),
                    request.birthDate(), request.cpf(), request.rg(), request.rgIssuer(),
                    request.gender(), request.maritalStatus(), request.professionalStatus());
            applyPatientFieldsCreate(companyId, contact, request.zipCode(), request.street(),
                    request.addressNumber(), request.addressComplement(), request.neighborhood(),
                    request.city(), request.state(), request.healthPlan(), request.healthPlanNumber(),
                    request.recordNumber(), request.referralSource(), request.hasGuardian(),
                    request.guardianName(), request.guardianCpf(), request.guardianPhone());
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

    /**
     * Busca da lista de pacientes: casa por nome, e-mail, telefone/celular,
     * numero do prontuario e CPF. Como CPF/telefone podem vir formatados,
     * tambem comparamos apenas os digitos (busca "111.222.333-44" casa com
     * "11122233344").
     */
    private static boolean matches(Contact c, String q) {
        if (containsIgnoreCase(c.getFirstName(), q)
                || containsIgnoreCase(c.getLastName(), q)
                || containsIgnoreCase(c.getEmail(), q)
                || containsIgnoreCase(c.getPhone(), q)
                || containsIgnoreCase(c.getMobile(), q)
                || containsIgnoreCase(c.getRecordNumber(), q)) {
            return true;
        }
        String digits = q.replaceAll("\\D", "");
        return !digits.isEmpty()
                && (digitsContain(c.getCpf(), digits)
                || digitsContain(c.getPhone(), digits)
                || digitsContain(c.getMobile(), digits)
                || digitsContain(c.getRecordNumber(), digits));
    }

    private static boolean containsIgnoreCase(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private static boolean digitsContain(String value, String digits) {
        return value != null && value.replaceAll("\\D", "").contains(digits);
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
            applyPatientFieldsUpdate(companyId, contact, request.zipCode(), request.street(),
                    request.addressNumber(), request.addressComplement(), request.neighborhood(),
                    request.city(), request.state(), request.healthPlan(), request.healthPlanNumber(),
                    request.recordNumber(), request.referralSource(), request.hasGuardian(),
                    request.guardianName(), request.guardianCpf(), request.guardianPhone());
            requireCpfIfConfigured(companyId, contact.getCpf());
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
                c.getZipCode(), c.getStreet(), c.getAddressNumber(), c.getAddressComplement(),
                c.getNeighborhood(), c.getCity(), c.getState(), c.getHealthPlan(),
                c.getHealthPlanNumber(), c.getRecordNumber(), c.getReferralSource(),
                c.isHasGuardian(), c.getGuardianName(), c.getGuardianCpf(), c.getGuardianPhone(),
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

    /** Mantém apenas dígitos (CEP/CPF/telefone); vazio → {@code null}. */
    private static String digitsOnly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private void applyPatientFieldsCreate(UUID companyId, Contact contact,
                                          String zipCode, String street, String addressNumber,
                                          String addressComplement, String neighborhood, String city,
                                          String state, String healthPlan, String healthPlanNumber,
                                          String recordNumber, String referralSource,
                                          Boolean hasGuardian, String guardianName,
                                          String guardianCpf, String guardianPhone) {
        contact.setZipCode(digitsOnly(zipCode));
        contact.setStreet(blankToNull(street));
        contact.setAddressNumber(blankToNull(addressNumber));
        contact.setAddressComplement(blankToNull(addressComplement));
        contact.setNeighborhood(blankToNull(neighborhood));
        contact.setCity(blankToNull(city));
        contact.setState(blankToNull(state));
        contact.setHealthPlan(blankToNull(healthPlan));
        contact.setHealthPlanNumber(blankToNull(healthPlanNumber));
        contact.setReferralSource(blankToNull(referralSource));
        String normalizedRecord = blankToNull(recordNumber);
        assertRecordNumberAvailable(companyId, normalizedRecord, null);
        contact.setRecordNumber(normalizedRecord);
        applyGuardian(contact, Boolean.TRUE.equals(hasGuardian), blankToNull(guardianName),
                digitsOnly(guardianCpf), digitsOnly(guardianPhone));
    }

    private void applyPatientFieldsUpdate(UUID companyId, Contact contact,
                                          String zipCode, String street, String addressNumber,
                                          String addressComplement, String neighborhood, String city,
                                          String state, String healthPlan, String healthPlanNumber,
                                          String recordNumber, String referralSource,
                                          Boolean hasGuardian, String guardianName,
                                          String guardianCpf, String guardianPhone) {
        if (zipCode != null) contact.setZipCode(digitsOnly(zipCode));
        if (street != null) contact.setStreet(blankToNull(street));
        if (addressNumber != null) contact.setAddressNumber(blankToNull(addressNumber));
        if (addressComplement != null) contact.setAddressComplement(blankToNull(addressComplement));
        if (neighborhood != null) contact.setNeighborhood(blankToNull(neighborhood));
        if (city != null) contact.setCity(blankToNull(city));
        if (state != null) contact.setState(blankToNull(state));
        if (healthPlan != null) contact.setHealthPlan(blankToNull(healthPlan));
        if (healthPlanNumber != null) contact.setHealthPlanNumber(blankToNull(healthPlanNumber));
        if (referralSource != null) contact.setReferralSource(blankToNull(referralSource));
        if (recordNumber != null) {
            String normalizedRecord = blankToNull(recordNumber);
            assertRecordNumberAvailable(companyId, normalizedRecord, contact.getId());
            contact.setRecordNumber(normalizedRecord);
        }
        boolean anyGuardianField = guardianName != null || guardianCpf != null || guardianPhone != null;
        if (hasGuardian != null || anyGuardianField) {
            boolean effective = hasGuardian != null ? hasGuardian : contact.isHasGuardian();
            String name = guardianName != null ? blankToNull(guardianName) : contact.getGuardianName();
            String cpf = guardianCpf != null ? digitsOnly(guardianCpf) : contact.getGuardianCpf();
            String phone = guardianPhone != null ? digitsOnly(guardianPhone) : contact.getGuardianPhone();
            applyGuardian(contact, effective, name, cpf, phone);
        }
    }

    private void applyGuardian(Contact contact, boolean hasGuardian,
                               String guardianName, String guardianCpf, String guardianPhone) {
        if (!hasGuardian) {
            contact.setHasGuardian(false);
            contact.setGuardianName(null);
            contact.setGuardianCpf(null);
            contact.setGuardianPhone(null);
            return;
        }
        if (guardianName == null || guardianPhone == null) {
            throw new ContactValidationException("Informe o nome e o telefone do responsável.");
        }
        contact.setHasGuardian(true);
        contact.setGuardianName(guardianName);
        contact.setGuardianCpf(guardianCpf);
        contact.setGuardianPhone(guardianPhone);
    }

    private void assertRecordNumberAvailable(UUID companyId, String recordNumber, UUID excludeContactId) {
        if (recordNumber != null
                && contactRepository.existsByRecordNumber(companyId, recordNumber, excludeContactId)) {
            throw new ContactConflictException("Já existe um paciente com este nº de prontuário.");
        }
    }

    /** Preferência da empresa (Minha Empresa → Preferências): CPF obrigatório em contatos. */
    private void requireCpfIfConfigured(UUID companyId, String cpf) {
        boolean required = companySettingsRepository.findByCompanyId(companyId)
                .map(CompanySettings::isRequireContactCpf)
                .orElse(false);
        if (required && (cpf == null || cpf.isBlank())) {
            throw new ContactValidationException("CPF é obrigatório para contatos desta empresa.");
        }
    }
}
