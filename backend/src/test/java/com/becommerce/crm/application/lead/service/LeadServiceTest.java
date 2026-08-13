package com.becommerce.crm.application.lead.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.lead.dto.CreateLeadRequest;
import com.becommerce.crm.application.lead.dto.LeadResponse;
import com.becommerce.crm.application.lead.port.output.LeadRepository;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.lead.Lead;
import com.becommerce.crm.domain.lead.LeadSource;
import com.becommerce.crm.domain.lead.LeadStatus;
import com.becommerce.crm.domain.lead.exception.DuplicateLeadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock LeadRepository leadRepository;
    @Mock ContactRepository contactRepository;
    @Mock TenantAuditRecorder auditor;

    @InjectMocks LeadService leadService;

    private final UUID companyId = UUID.randomUUID();

    private Contact ownedContact() {
        return Contact.reconstitute(UUID.randomUUID(), companyId, "Ana", "Souza", "ana@e.com",
                null, null, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void shouldCreateLeadWhenContactOwnedAndUnique() {
        Contact contact = ownedContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(leadRepository.existsByContactIdAndCompanyId(contact.getId(), companyId)).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadResponse response = leadService.create(companyId,
                new CreateLeadRequest(contact.getId(), LeadStatus.NEW, 60, null, LeadSource.WHATSAPP,
                        null, null, "nota"), UUID.randomUUID());

        assertNotNull(response.id());
        assertEquals(contact.getId(), response.contactId());
        assertEquals(LeadStatus.NEW, response.status());
        assertEquals(60, response.score());
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void shouldRejectCreateWhenContactBelongsToAnotherCompany() {
        Contact foreign = Contact.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "Ana", "Souza",
                "ana@e.com", null, null, LocalDateTime.now(), LocalDateTime.now(), null);
        when(contactRepository.findById(foreign.getId())).thenReturn(java.util.Optional.of(foreign));

        assertThrows(ContactNotFoundException.class, () -> leadService.create(companyId,
                new CreateLeadRequest(foreign.getId(), null, null, null, LeadSource.MANUAL, null, null, null),
                UUID.randomUUID()));
        verify(leadRepository, never()).save(any(Lead.class));
    }

    @Test
    void shouldRejectDuplicateLeadForSameContact() {
        Contact contact = ownedContact();
        when(contactRepository.findById(contact.getId())).thenReturn(java.util.Optional.of(contact));
        when(leadRepository.existsByContactIdAndCompanyId(contact.getId(), companyId)).thenReturn(true);

        assertThrows(DuplicateLeadException.class, () -> leadService.create(companyId,
                new CreateLeadRequest(contact.getId(), null, null, null, LeadSource.API, null, null, null),
                UUID.randomUUID()));
        verify(leadRepository, never()).save(any(Lead.class));
    }

    @Test
    void shouldThrowWhenLeadNotFoundOnGet() {
        when(leadRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.empty());
        assertThrows(com.becommerce.crm.domain.lead.exception.LeadNotFoundException.class,
                () -> leadService.getById(companyId, UUID.randomUUID()));
    }
}