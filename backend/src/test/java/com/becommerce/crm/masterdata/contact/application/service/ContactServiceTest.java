package com.becommerce.crm.masterdata.contact.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.company.application.service.CompanyQuotaService;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.dto.request.CreateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
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

    @InjectMocks ContactService contactService;

    private final UUID companyId = UUID.randomUUID();

    @Test
    void shouldCreateContactWhenBelowLimit() {
        doNothing().when(quotaService).assertCanAddContact(companyId);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.create(
                companyId, new CreateContactRequest("Ana", "Souza", "ana@empresa.com", "119",
                        null, "nota", null, null, null, null, null, null, null), UUID.randomUUID());

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
                () -> contactService.create(companyId, new CreateContactRequest("Ana", "Souza", "ana@empresa.com",
                        null, null, null, null, null, null, null, null, null, null), UUID.randomUUID()));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    // ===== Sprint 20 — autorização granular por campo (piloto Contatos) =====

    private Contact existingContact() {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza",
                "ana@empresa.com", "11999990000", null, null,
                null, null, null, null, null, null, null,
                null, null, null);
    }

    @Test
    void updateShouldDenyEmailChangeWithoutFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(authorities.has("contact:field:email:update")).thenReturn(false);

        assertThrows(CrmAccessDeniedException.class, () -> contactService.update(
                companyId, contact.getId(),
                new UpdateContactRequest(null, null, "novo@empresa.com", null,
                        null, null, null, null, null, null, null, null, null)));

        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void updateShouldAllowEmailChangeWithFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(authorities.has("contact:field:email:update")).thenReturn(true);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = contactService.update(companyId, contact.getId(),
                new UpdateContactRequest(null, null, "novo@empresa.com", null,
                        null, null, null, null, null, null, null, null, null));

        assertEquals("novo@empresa.com", response.email());
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void updateShouldDenyPhoneChangeWithoutFieldPermission() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(authorities.has("contact:field:phone:update")).thenReturn(false);

        assertThrows(CrmAccessDeniedException.class, () -> contactService.update(
                companyId, contact.getId(),
                new UpdateContactRequest(null, null, null, "11888887777",
                        null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void updateShouldNotRequireFieldPermissionWhenValueUnchanged() {
        Contact contact = existingContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = contactService.update(companyId, contact.getId(),
                new UpdateContactRequest(null, null, "ana@empresa.com", null,
                        null, null, null, null, null, null, null, null, null));

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
                new UpdateContactRequest("Ana Maria", null, null, null,
                        null, "obs", null, null, null, null, null, null, null));

        assertEquals("Ana Maria", response.firstName());
        verify(contactRepository).save(any(Contact.class));
    }
}
