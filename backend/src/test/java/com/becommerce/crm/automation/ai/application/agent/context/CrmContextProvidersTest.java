package com.becommerce.crm.automation.ai.application.agent.context;

import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanySettingsRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanySettings;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Contexto da clínica e do paciente vêm do CRM, com isolamento por empresa. */
class CrmContextProvidersTest {

    private final UUID companyId = UUID.randomUUID();

    @Test
    void clinic_usesCompanyDataAndConfiguredTimezone() {
        CompanyRepository companies = mock(CompanyRepository.class);
        CompanySettingsRepository settings = mock(CompanySettingsRepository.class);
        Company company = mock(Company.class);
        when(company.getTradingName()).thenReturn("Clínica Sorriso");
        when(company.getPhone()).thenReturn("(11) 4000-0000");
        when(company.getAddressStreet()).thenReturn("Rua A");
        when(company.getAddressNumber()).thenReturn("10");
        when(company.getAddressCity()).thenReturn("Manaus");
        when(company.getAddressState()).thenReturn("AM");
        CompanySettings cs = mock(CompanySettings.class);
        when(cs.getTimezone()).thenReturn("America/Manaus");
        when(cs.getBusinessHours()).thenReturn("Seg a sex 08-18");
        when(companies.findById(companyId)).thenReturn(Optional.of(company));
        when(settings.findByCompanyId(companyId)).thenReturn(Optional.of(cs));

        ClinicContext ctx = new ClinicContextProvider(companies, settings,
                Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneId.of("UTC"))).load(companyId);

        assertEquals("Clínica Sorriso", ctx.name());
        assertEquals("Rua A, 10 - Manaus/AM", ctx.address());
        assertEquals("Seg a sex 08-18", ctx.businessHours());
        assertEquals(ZoneId.of("America/Manaus"), ctx.zone());
        assertEquals(8, ctx.now().getHour());
    }

    @Test
    void clinic_degradesToDefaultsWhenCrmFails() {
        CompanyRepository companies = mock(CompanyRepository.class);
        CompanySettingsRepository settings = mock(CompanySettingsRepository.class);
        when(companies.findById(any())).thenThrow(new RuntimeException("down"));
        when(settings.findByCompanyId(any())).thenReturn(Optional.empty());

        ClinicContext ctx = new ClinicContextProvider(companies, settings).load(companyId);

        assertNull(ctx.name());
        assertEquals(ZoneId.of("America/Sao_Paulo"), ctx.zone());
    }

    @Test
    void patient_ignoresContactFromAnotherCompany() {
        ContactRepository contacts = mock(ContactRepository.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        UUID contactId = UUID.randomUUID();
        Contact foreign = mock(Contact.class);
        when(foreign.getCompanyId()).thenReturn(UUID.randomUUID());
        when(contacts.findById(contactId)).thenReturn(Optional.of(foreign));

        PatientContext ctx = new PatientContextProvider(contacts, appointments)
                .load(companyId, contactId, null, "Perfil", clinic());

        assertFalse(ctx.isIdentified());
        verify(appointments, never()).findUpcomingByContact(any(), any(), any(), anyInt());
    }

    @Test
    void patient_resolvesByPhoneAndLoadsUpcomingFromAgenda() {
        ContactRepository contacts = mock(ContactRepository.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        UUID contactId = UUID.randomUUID();
        Contact contact = mock(Contact.class);
        when(contact.getId()).thenReturn(contactId);
        when(contact.getCompanyId()).thenReturn(companyId);
        when(contact.getFirstName()).thenReturn("Carlos");
        when(contact.getLastName()).thenReturn("Silva");
        when(contacts.findByCompanyIdAndPhone(eq(companyId), any())).thenReturn(Optional.empty());
        when(contacts.findByCompanyIdAndPhone(companyId, "5511999990000")).thenReturn(Optional.of(contact));
        when(appointments.findUpcomingByContact(eq(companyId), eq(contactId), any(), eq(3))).thenReturn(List.of());

        PatientContext ctx = new PatientContextProvider(contacts, appointments)
                .load(companyId, null, "5511999990000", "Carlos", clinic());

        assertEquals(contactId, ctx.contactId());
        assertEquals("Carlos Silva", ctx.name());
    }

    private static ClinicContext clinic() {
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        return new ClinicContext(null, null, null, null, zone, ZonedDateTime.now(zone));
    }
}
