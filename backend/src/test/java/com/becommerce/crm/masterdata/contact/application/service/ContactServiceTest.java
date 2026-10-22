package com.becommerce.crm.masterdata.contact.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.service.CompanyQuotaService;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.quota.domain.exception.QuotaExceededException;
import com.becommerce.crm.shared.security.authorization.CurrentUserAuthorities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock ContactRepository contactRepository;
    @Mock CompanyQuotaService quotaService;
    @Mock TenantAuditRecorder auditor;
    @Mock com.becommerce.crm.identity.application.port.output.EventPublisher eventPublisher;
    @Mock CurrentUserAuthorities authorities;

    @Mock com.becommerce.crm.masterdata.company.application.port.output.CompanySettingsRepository companySettingsRepository;
    @InjectMocks ContactService contactService;

    private final UUID companyId = UUID.randomUUID();

    // ===== helpers (28 campos: 13 base + 15 da ficha) =====

    private static CreateContactRequest createRequest(String firstName, String lastName, String email,
                                                      String phone, String notes) {
        return new CreateContactRequest(firstName, lastName, email, phone, null, notes, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }

    private static CreateContactRequest createPatientRequest(boolean hasGuardian, String guardianName,
                                                             String guardianCpf, String guardianPhone,
                                                             String recordNumber, String zipCode,
                                                             String state) {
        return new CreateContactRequest("Ana", "Souza", "ana@empresa.com", null, null, null, null,
                null, null, null, null, null, null,
                zipCode, "Av Paulista", "1000", null, "Bela Vista", "São Paulo", state, "Unimed",
                "12345", recordNumber, "Indicação", hasGuardian, guardianName, guardianCpf, guardianPhone);
    }

    private static UpdateContactRequest updateWithEmail(String email) {
        return new UpdateContactRequest(null, null, email, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private static UpdateContactRequest updateWithPhone(String phone) {
        return new UpdateContactRequest(null, null, null, phone, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private static UpdateContactRequest updateWithFirstNameAndNotes(String firstName, String notes) {
        return new UpdateContactRequest(firstName, null, null, null, null, notes, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    private static UpdateContactRequest updateWithHealthPlan(String healthPlan) {
        return new UpdateContactRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, healthPlan, null, null, null,
                null, null, null, null);
    }

    private static UpdateContactRequest updateWithRecordNumber(String recordNumber) {
        return new UpdateContactRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, recordNumber, null,
                null, null, null, null);
    }

    // ===== criação =====

    @Test
    void shouldCreateContactWhenBelowLimit() {
        doNothing().when(quotaService).assertCanAddContact(companyId);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.create(
                companyId, createRequest("Ana", "Souza", "ana@empresa.com", "119", "nota"),
                UUID.randomUUID());

        assertNotNull(response.id());
        assertEquals("Ana", response.firstName());
        assertEquals("ana@empresa.com", response.email());
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void shouldBlockContactWhenLimitReached() {
        doThrow(new QuotaExceededException("Limite de contatos da empresa atingido (500)."))
                .when(quotaService).assertCanAddContact(companyId);

        assertThrows(QuotaExceededException.class,
                () -> contactService.create(companyId,
                        createRequest("Ana", "Souza", "ana@empresa.com", null, null),
                        UUID.randomUUID()));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    // ===== ficha do paciente (V097) — cadastro, responsável e prontuário =====

    @Test
    void shouldCreatePatientWithAddressAndGuardianNormalized() {
        doNothing().when(quotaService).assertCanAddContact(companyId);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.create(companyId,
                createPatientRequest(true, "Maria Souza", "111.222.333-44", "(11) 98888-7777",
                        "PRT-01", "01310-100", "SP"),
                UUID.randomUUID());

        assertEquals("01310100", response.zipCode());
        assertEquals("Av Paulista", response.street());
        assertEquals("SP", response.state());
        assertEquals("PRT-01", response.recordNumber());
        assertTrue(response.hasGuardian());
        assertEquals("Maria Souza", response.guardianName());
        assertEquals("11122233344", response.guardianCpf());
        assertEquals("11988887777", response.guardianPhone());
    }

    @Test
    void shouldRequireGuardianNameAndPhoneWhenHasGuardian() {
        doNothing().when(quotaService).assertCanAddContact(companyId);

        assertThrows(ContactValidationException.class, () -> contactService.create(companyId,
                createPatientRequest(true, "Maria", null, null, null, null, null),
                UUID.randomUUID()));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void shouldClearGuardianDataWhenHasGuardianFalse() {
        doNothing().when(quotaService).assertCanAddContact(companyId);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.create(companyId,
                createPatientRequest(false, "Maria", "11122233344", "11988887777", null, null, null),
                UUID.randomUUID());

        assertFalse(response.hasGuardian());
        assertNull(response.guardianName());
        assertNull(response.guardianCpf());
        assertNull(response.guardianPhone());
    }

    @Test
    void shouldRejectDuplicateRecordNumber() {
        doNothing().when(quotaService).assertCanAddContact(companyId);
        when(contactRepository.existsByRecordNumber(companyId, "PRT-01", null)).thenReturn(true);

        assertThrows(ContactConflictException.class, () -> contactService.create(companyId,
                createPatientRequest(false, null, null, null, "PRT-01", null, null),
                UUID.randomUUID()));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    // ===== atualização =====

    @Test
    void updateShouldAllowOwnRecordNumber() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.existsByRecordNumber(companyId, "PRT-01", contact.getId())).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.update(companyId, contact.getId(),
                updateWithRecordNumber("PRT-01"));

        assertEquals("PRT-01", response.recordNumber());
    }

    @Test
    void updateShouldRejectDuplicateRecordNumber() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.existsByRecordNumber(companyId, "PRT-02", contact.getId())).thenReturn(true);

        assertThrows(ContactConflictException.class,
                () -> contactService.update(companyId, contact.getId(), updateWithRecordNumber("PRT-02")));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void updateShouldClearFieldWhenEmptyStringSent() {
        Contact contact = existingContact();
        contact.setHealthPlan("Unimed");
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.update(companyId, contact.getId(),
                updateWithHealthPlan(""));

        assertNull(response.healthPlan());
    }

    private Contact existingContact() {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza",
                "ana@empresa.com", "11999990000", null, null,
                null, null, null, null, null, null, null,
                null, null, null);
    }

    // ===== Sprint 20 — autorização granular por campo (piloto Contatos) =====

    @Test
    void updateShouldDenyEmailChangeWithoutFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(authorities.has("contact:field:email:update")).thenReturn(false);

        assertThrows(CrmAccessDeniedException.class, () -> contactService.update(
                companyId, contact.getId(), updateWithEmail("novo@empresa.com")));

        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void updateShouldAllowEmailChangeWithFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(authorities.has("contact:field:email:update")).thenReturn(true);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = contactService.update(companyId, contact.getId(),
                updateWithEmail("novo@empresa.com"));

        assertEquals("novo@empresa.com", response.email());
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void updateShouldDenyPhoneChangeWithoutFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(authorities.has("contact:field:phone:update")).thenReturn(false);

        assertThrows(CrmAccessDeniedException.class, () -> contactService.update(
                companyId, contact.getId(), updateWithPhone("11888887777")));
    }

    @Test
    void updateShouldNotRequireFieldPermissionWhenValueUnchanged() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = contactService.update(companyId, contact.getId(),
                updateWithEmail("ana@empresa.com"));

        assertEquals("ana@empresa.com", response.email());
        verify(authorities, never()).has(anyString());
        verify(authorities, never()).has("contact:field:email:update");
    }

    @Test
    void updateShouldAllowOtherFieldsWithoutFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = contactService.update(companyId, contact.getId(),
                updateWithFirstNameAndNotes("Ana Maria", "obs"));

        assertEquals("Ana Maria", response.firstName());
        verify(contactRepository).save(any(Contact.class));
    }

    // ===== busca da lista de pacientes (nome, CPF, celular, prontuario) =====

    @Test
    void searchShouldMatchByNameCpfDigitsMobileAndRecordNumber() {
        Contact ana = Contact.create(companyId, "Ana", "Souza", "ana@empresa.com", "1130001111",
                "11988887777", null, null, "11122233344", null, null, null, null, null);
        ana.setRecordNumber("PRT-42");
        Contact bruno = Contact.create(companyId, "Bruno", "Lima", "bruno@empresa.com", "2130002222",
                "21977776666", null, null, null, null, null, null, null, null);
        when(contactRepository.findByCompanyIdActive(companyId))
                .thenReturn(java.util.List.of(ana, bruno));

        assertEquals(1, contactService.search(companyId, "111.222.333-44", 10).size());
        assertEquals(1, contactService.search(companyId, "988887", 10).size());
        assertEquals(1, contactService.search(companyId, "prt-42", 10).size());
        assertEquals(2, contactService.search(companyId, "empresa.com", 10).size());
        assertEquals(1, contactService.search(companyId, "bruno", 10).size());
    }

    // ===== Módulo Pacientes (V104) =====

    @Test
    void createPatientShouldMarkContactAsPatientSinceToday() {
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.createPatient(companyId,
                createRequest("Ana", "Souza", "ana@empresa.com", null, null), UUID.randomUUID());

        assertTrue(response.isPatient());
        assertEquals(java.time.LocalDate.now(), response.patientSince());
    }

    @Test
    void createShouldNotMarkPlainContactAsPatient() {
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.create(companyId,
                createRequest("Ana", "Souza", "ana@empresa.com", null, null), UUID.randomUUID());

        assertFalse(response.isPatient());
        assertNull(response.patientSince());
    }

    @Test
    void makePatientShouldBeIdempotentAndKeepOriginalDate() {
        Contact contact = existingContact();
        java.time.LocalDate since = java.time.LocalDate.of(2025, 1, 10);
        contact.restorePatient(true, since);
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));

        ContactResponse response = contactService.makePatient(companyId, contact.getId());

        assertTrue(response.isPatient());
        assertEquals(since, response.patientSince());
        verify(contactRepository, never()).save(any());
    }

    @Test
    void makePatientShouldMarkContact() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.makePatient(companyId, contact.getId());

        assertTrue(response.isPatient());
        assertEquals(java.time.LocalDate.now(), response.patientSince());
    }

    @Test
    void getPatientShouldReturn404ForContactThatIsNotPatient() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));

        assertThrows(com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException.class,
                () -> contactService.getPatient(companyId, contact.getId()));
    }

    @Test
    void searchPatientsShouldFilterByRecordNumberAndCpf() {
        Contact ana = existingContact();
        ana.setRecordNumber("PRT-7");
        ana.setCpf("11122233344");
        ana.restorePatient(true, java.time.LocalDate.now());
        Contact bruno = Contact.reconstitute(UUID.randomUUID(), companyId, "Bruno", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        bruno.restorePatient(true, java.time.LocalDate.now());
        when(contactRepository.findPatientsByCompanyIdActive(companyId)).thenReturn(java.util.List.of(ana, bruno));

        assertEquals(2, contactService.searchPatients(companyId, null).size());
        assertEquals(1, contactService.searchPatients(companyId, "prt-7").size());
        assertEquals(1, contactService.searchPatients(companyId, "111.222.333-44").size());
    }

    @Test
    void markAsPatientIfExistsShouldIgnoreMissingContactWithoutThrowing() {
        UUID missing = UUID.randomUUID();
        when(contactRepository.findById(missing)).thenReturn(java.util.Optional.empty());

        assertFalse(contactService.markAsPatientIfExists(companyId, missing));
        verify(contactRepository, never()).save(any());
    }
}
