package com.becommerce.crm.application.contact.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.company.service.CompanyQuotaService;
import com.becommerce.crm.application.contact.dto.ContactResponse;
import com.becommerce.crm.application.contact.dto.CreateContactRequest;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock ContactRepository contactRepository;
    @Mock CompanyQuotaService quotaService;
    @Mock TenantAuditRecorder auditor;
    @Mock com.becommerce.crm.application.identity.port.output.EventPublisher eventPublisher;

    @InjectMocks ContactService contactService;

    private final UUID companyId = UUID.randomUUID();

    @Test
    void shouldCreateContactWhenBelowLimit() {
        doNothing().when(quotaService).assertCanAddContact(companyId);
        when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactResponse response = contactService.create(
                companyId, new CreateContactRequest("Ana", "Souza", "ana@empresa.com", "119", "nota"), UUID.randomUUID());

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
                () -> contactService.create(companyId, new CreateContactRequest("Ana", "Souza", "ana@empresa.com", null, null), UUID.randomUUID()));
        verify(contactRepository, never()).save(any(Contact.class));
    }
}