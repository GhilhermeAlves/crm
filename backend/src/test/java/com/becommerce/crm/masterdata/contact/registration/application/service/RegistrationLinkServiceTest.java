package com.becommerce.crm.masterdata.contact.registration.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.contact.application.dto.request.UpdateContactRequest;
import com.becommerce.crm.masterdata.contact.application.dto.response.ContactResponse;
import com.becommerce.crm.masterdata.contact.application.port.in.ContactUseCase;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactNotFoundException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import com.becommerce.crm.masterdata.contact.registration.application.dto.PatientRegistrationRequest;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationLinkResponse;
import com.becommerce.crm.masterdata.contact.registration.application.dto.RegistrationPreviewResponse;
import com.becommerce.crm.masterdata.contact.registration.application.port.RegistrationLinkRepository;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNoLongerValidException;
import com.becommerce.crm.masterdata.contact.registration.domain.exception.RegistrationLinkNotFoundException;
import com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence.RegistrationTokenContextHolder;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationLinkServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final String TOKEN = "token-cru-do-paciente";
    private static final String VALID_CPF = "529.982.247-25";

    @Mock RegistrationLinkRepository repository;
    @Mock RegistrationTokenContextHolder tokenContext;
    @Mock ContactUseCase contactUseCase;
    @Mock CompanyRepository companyRepository;
    @Mock TenantAuditRecorder auditor;

    private RegistrationLinkService service;
    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RegistrationLinkService(repository, tokenContext, contactUseCase, companyRepository,
                auditor, "https://app.clinica.com/", CLOCK);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private RegistrationLink link(LocalDateTime expiresAt, LocalDateTime usedAt) {
        return new RegistrationLink(UUID.randomUUID(), companyId, contactId, RegistrationLink.hash(TOKEN),
                expiresAt, usedAt, actorId, NOW.minusDays(1));
    }

    private PatientRegistrationRequest request(String cpf, String website) {
        return new PatientRegistrationRequest("Maria", "Silva", cpf, LocalDate.of(1990, 5, 1),
                "maria@ex.com", "(11) 98888-7777",
                new PatientRegistrationRequest.Address("01310-100", "Av. Paulista", "1000", "ap 1",
                        "Bela Vista", "São Paulo", "SP"),
                website);
    }

    @Test
    void create_invalidatesPreviousAndReturnsUrlWithRawToken() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrationLinkResponse response = service.create(companyId, contactId, actorId);

        InOrder order = inOrder(contactUseCase, repository);
        order.verify(contactUseCase).getById(companyId, contactId);
        order.verify(repository).invalidateUnused(companyId, contactId, NOW);
        ArgumentCaptor<RegistrationLink> saved = ArgumentCaptor.forClass(RegistrationLink.class);
        order.verify(repository).save(saved.capture());

        assertTrue(response.url().startsWith("https://app.clinica.com/cadastro/"));
        String rawToken = response.url().substring("https://app.clinica.com/cadastro/".length());
        assertEquals(43, rawToken.length());
        assertEquals(RegistrationLink.hash(rawToken), saved.getValue().getTokenHash());
        assertEquals(NOW.plusDays(7), response.expiresAt());
        assertEquals(actorId, saved.getValue().getCreatedBy());
        assertNull(saved.getValue().getUsedAt());
        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void create_unknownContact_propagates404() {
        when(contactUseCase.getById(companyId, contactId)).thenThrow(new ContactNotFoundException(contactId));
        assertThrows(ContactNotFoundException.class, () -> service.create(companyId, contactId, actorId));
        verify(repository, never()).save(any());
    }

    @Test
    void preview_returnsCompanyAndContactData_andClearsContext() {
        when(repository.findByTokenHash(RegistrationLink.hash(TOKEN)))
                .thenReturn(Optional.of(link(NOW.plusDays(3), null)));
        Company company = mock(Company.class);
        when(company.getTradingName()).thenReturn("Clínica Sorriso");
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        ContactResponse contact = mock(ContactResponse.class);
        when(contact.firstName()).thenReturn("Maria");
        when(contact.lastName()).thenReturn("Silva");
        when(contact.mobile()).thenReturn("11988887777");
        when(contactUseCase.getById(companyId, contactId)).thenReturn(contact);

        RegistrationPreviewResponse preview = service.preview(TOKEN);

        assertEquals(new RegistrationPreviewResponse("Clínica Sorriso", "Maria", "Silva", "11988887777"), preview);
        verify(tokenContext).setTokenHash(RegistrationLink.hash(TOKEN));
        verify(tokenContext).clear();
    }

    @Test
    void preview_unknownToken_404() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(RegistrationLinkNotFoundException.class, () -> service.preview(TOKEN));
        verify(tokenContext).clear();
    }

    @Test
    void preview_expired_410() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(link(NOW.minusMinutes(1), null)));
        assertThrows(RegistrationLinkNoLongerValidException.class, () -> service.preview(TOKEN));
    }

    @Test
    void preview_used_410() {
        when(repository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(link(NOW.plusDays(3), NOW.minusHours(1))));
        assertThrows(RegistrationLinkNoLongerValidException.class, () -> service.preview(TOKEN));
    }

    @Test
    void submit_updatesContactMarksUsedAndAudits() {
        RegistrationLink link = link(NOW.plusDays(3), null);
        when(repository.findByTokenHashForUpdate(RegistrationLink.hash(TOKEN))).thenReturn(Optional.of(link));

        service.submit(TOKEN, request(VALID_CPF, null));

        ArgumentCaptor<UpdateContactRequest> update = ArgumentCaptor.forClass(UpdateContactRequest.class);
        verify(contactUseCase).updateFromRegistrationLink(eq(companyId), eq(contactId), update.capture());
        assertEquals("52998224725", update.getValue().cpf());
        assertEquals("11988887777", update.getValue().mobile());
        assertEquals("01310100", update.getValue().zipCode());
        assertEquals("1000", update.getValue().addressNumber());
        assertEquals("SP", update.getValue().state());
        verify(repository).markUsed(link.getId(), NOW);
        verify(auditor).record(eq(companyId), eq(AuditAction.UPDATE), eq(AuditModule.CONTACTS), eq("Contact"),
                eq(contactId.toString()), eq("Cadastro preenchido pelo paciente pelo link."), isNull(), anyMap());
        verify(tokenContext).clear();
        assertNull(TenantContext.getCompanyId());
    }

    @Test
    void submit_invalidCpf_400_withoutTouchingData() {
        assertThrows(ContactValidationException.class, () -> service.submit(TOKEN, request("111.222.333-44", null)));
        verify(contactUseCase, never()).updateFromRegistrationLink(any(), any(), any());
        verify(repository, never()).markUsed(any(), any());
    }

    @Test
    void submit_honeypotFilled_rejected() {
        assertThrows(ContactValidationException.class, () -> service.submit(TOKEN, request(VALID_CPF, "http://spam")));
        verify(repository, never()).findByTokenHashForUpdate(anyString());
    }

    @Test
    void submit_usedLink_410() {
        when(repository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(link(NOW.plusDays(3), NOW.minusHours(1))));
        assertThrows(RegistrationLinkNoLongerValidException.class, () -> service.submit(TOKEN, request(VALID_CPF, null)));
        verify(contactUseCase, never()).updateFromRegistrationLink(any(), any(), any());
    }

    @Test
    void submit_unknownToken_404() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());
        assertThrows(RegistrationLinkNotFoundException.class, () -> service.submit(TOKEN, request(VALID_CPF, null)));
    }

    @Test
    void submit_blankFields_meanDoNotChange() {
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(link(NOW.plusDays(3), null)));
        PatientRegistrationRequest req = new PatientRegistrationRequest("Maria", "  ", VALID_CPF, null, "", "",
                new PatientRegistrationRequest.Address("", " ", "", "", "", "", ""), null);

        service.submit(TOKEN, req);

        ArgumentCaptor<UpdateContactRequest> update = ArgumentCaptor.forClass(UpdateContactRequest.class);
        verify(contactUseCase).updateFromRegistrationLink(eq(companyId), eq(contactId), update.capture());
        UpdateContactRequest u = update.getValue();
        assertNull(u.email());
        assertNull(u.lastName());
        assertNull(u.mobile());
        assertNull(u.zipCode());
        assertNull(u.street());
        assertNull(u.city());
        assertNull(u.state());
    }

    @Test
    void cpfCheckDigits() {
        assertTrue(RegistrationLinkService.isValidCpf("52998224725"));
        assertFalse(RegistrationLinkService.isValidCpf("52998224724"));
        assertFalse(RegistrationLinkService.isValidCpf("11111111111"));
        assertFalse(RegistrationLinkService.isValidCpf("123"));
    }
}
