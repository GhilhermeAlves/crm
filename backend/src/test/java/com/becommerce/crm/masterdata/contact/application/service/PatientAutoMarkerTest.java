package com.becommerce.crm.masterdata.contact.application.service;

import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientAutoMarkerTest {

    private final CompanyModulesService modules = mock(CompanyModulesService.class);
    private final ContactUseCase contacts = mock(ContactUseCase.class);
    private final PatientAutoMarker marker = new PatientAutoMarker(modules, contacts);
    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();

    @Test
    void shouldMarkContactWhenPatientsModuleEnabled() {
        when(modules.isEnabled(companyId, CompanyModule.PATIENTS)).thenReturn(true);
        when(contacts.markAsPatientIfExists(companyId, contactId)).thenReturn(true);

        assertTrue(marker.markOnAppointment(companyId, contactId));
        verify(contacts).markAsPatientIfExists(companyId, contactId);
    }

    @Test
    void shouldNotMarkOutsideHealthCompanies() {
        when(modules.isEnabled(companyId, CompanyModule.PATIENTS)).thenReturn(false);

        assertFalse(marker.markOnAppointment(companyId, contactId));
        verify(contacts, never()).markAsPatientIfExists(any(), any());
    }

    @Test
    void shouldIgnoreAppointmentWithoutContact() {
        assertFalse(marker.markOnAppointment(companyId, null));
        verify(contacts, never()).markAsPatientIfExists(any(), any());
    }
}
