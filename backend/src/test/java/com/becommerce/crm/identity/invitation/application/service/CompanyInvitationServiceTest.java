package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.communication.notification.application.EmailSender;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.service.AuthService;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.valueobject.Email;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CompanyDetails;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CompanyPrefill;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CreateRequest;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.LinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.PreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.Response;
import com.becommerce.crm.identity.invitation.application.port.output.CompanyInvitationRepository;
import com.becommerce.crm.identity.invitation.domain.CompanyInvitation;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationRuleViolationException;
import com.becommerce.crm.identity.invitation.infrastructure.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import com.becommerce.crm.masterdata.company.application.dto.CompanyResponse;
import com.becommerce.crm.masterdata.company.application.dto.CreateCompanyRequest;
import com.becommerce.crm.masterdata.company.application.port.input.CompanyUseCase;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyInvitationServiceTest {

    @Mock CompanyInvitationRepository repository;
    @Mock CompanyUseCase companyUseCase;
    @Mock UserRepository userRepository;
    @Mock AuthService authService;
    @Mock EmailSender emailSender;
    @Mock InvitationTokenContextHolder tokenContext;
    @Mock InvitationRateLimiter rateLimiter;
    @Mock TenantAuditRecorder auditor;

    private CompanyInvitationService service;
    private final UUID adminCompanyId = UUID.randomUUID();
    private final UUID crmAdminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CompanyInvitationService(repository, companyUseCase, userRepository, authService,
                emailSender, tokenContext, rateLimiter, auditor, "");
        lenient().when(rateLimiter.tryCreate(anyString())).thenReturn(true);
        lenient().when(rateLimiter.tryAccept(anyString())).thenReturn(true);
        lenient().when(repository.save(any(CompanyInvitation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static CreateRequest request(String segment, String plan, Boolean sendEmail) {
        return new CreateRequest("Dono@Clinica.com", "Dono", segment, List.of("CRM", "AGENDA"), plan, 3, sendEmail,
                null);
    }

    private static CompanyDetails details() {
        return new CompanyDetails("Clínica Sorriso Ltda", "Clínica Sorriso", "12345678000199",
                "contato@sorriso.com", "11999990000", "01001000", "Rua A", "10", null,
                "Centro", "São Paulo", "SP");
    }

    private CompanyInvitation pendingInvitation() {
        return CompanyInvitation.create(adminCompanyId, "dono@clinica.com", "Dono", "DENTAL", "CRM,AGENDA",
                "PROFESSIONAL", 3, null, InvitationTokenService.hash("tok"), crmAdminId);
    }

    @Test
    void createsInvitationWithWhatTheCrmAdminChose() {
        LinkResponse response = service.create(adminCompanyId, request("dental", "professional", true), crmAdminId);

        assertEquals("dono@clinica.com", response.invitation().email());
        assertEquals("DENTAL", response.invitation().segment());
        assertEquals("PROFESSIONAL", response.invitation().plan());
        assertEquals(List.of("AGENDA", "CRM"), response.invitation().enabledModules());
        assertEquals(3, response.invitation().maxUsers());
        assertTrue(response.url().startsWith("/convite-empresa/"));
        verify(emailSender).sendInvitation(eq("dono@clinica.com"), anyString(), eq("ADMIN"), anyString());
    }

    @Test
    void createsInvitationWithoutEmailForWhatsApp() {
        service.create(adminCompanyId, request("DENTAL", "STARTER", false), crmAdminId);

        verify(emailSender, never()).sendInvitation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rejectsUnknownSegmentOrPlan() {
        assertThrows(InvitationRuleViolationException.class,
                () -> service.create(adminCompanyId, request("PADARIA", "STARTER", true), crmAdminId));
        assertThrows(InvitationRuleViolationException.class,
                () -> service.create(adminCompanyId, request("DENTAL", "GOLD", true), crmAdminId));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsSecondPendingInvitationForSameEmail() {
        when(repository.findByInviterCompanyId(adminCompanyId)).thenReturn(List.of(pendingInvitation()));

        assertThrows(InvitationRuleViolationException.class,
                () -> service.create(adminCompanyId, request("DENTAL", "STARTER", true), crmAdminId));
    }

    @Test
    void acceptCreatesCompanyOwnedByInviteeWithInvitationSettings() {
        UUID userId = UUID.randomUUID();
        UUID newCompanyId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(new Email("dono@clinica.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(pendingInvitation()));
        CompanyResponse company = mock(CompanyResponse.class);
        when(company.id()).thenReturn(newCompanyId.toString());
        when(companyUseCase.createCompany(any(CreateCompanyRequest.class), eq(userId))).thenReturn(company);

        Response response = service.accept("tok", details(), userId);

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        assertEquals(newCompanyId, response.companyId());
        ArgumentCaptor<CreateCompanyRequest> captor = ArgumentCaptor.forClass(CreateCompanyRequest.class);
        verify(companyUseCase).createCompany(captor.capture(), eq(userId));
        CreateCompanyRequest created = captor.getValue();
        assertEquals("DENTAL", created.segment());
        assertEquals("PROFESSIONAL", created.plan());
        assertEquals(3, created.maxUsers());
        assertEquals(List.of("AGENDA", "CRM"), created.enabledModules());
        assertEquals("Clínica Sorriso", created.tradingName());
    }

    @Test
    void createsInvitationWithPrefilledCompanyData() {
        CompanyPrefill prefill = new CompanyPrefill("Clínica Sorriso Ltda", "Clínica Sorriso", " ", null,
                "11999990000", null, null, null, null, null, "São Paulo", "SP");
        CreateRequest withPrefill = new CreateRequest("dono@clinica.com", "Dono", "DENTAL", List.of("CRM"),
                "STARTER", 3, false, prefill);

        LinkResponse response = service.create(adminCompanyId, withPrefill, crmAdminId);

        CompanyPrefill saved = response.invitation().company();
        assertEquals("Clínica Sorriso", saved.tradingName());
        assertEquals("São Paulo", saved.addressCity());
        // Campos em branco não são guardados.
        assertNull(saved.cnpj());
        assertNull(saved.email());
    }

    @Test
    void previewReturnsPrefilledCompanyData() {
        CompanyInvitation invitation = CompanyInvitation.create(adminCompanyId, "dono@clinica.com", "Dono",
                "DENTAL", "CRM", "STARTER", 3, Map.of("tradingName", "Clínica Sorriso"),
                InvitationTokenService.hash("tok"), crmAdminId);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));

        PreviewResponse preview = service.preview("tok");

        assertEquals("Clínica Sorriso", preview.company().tradingName());
    }

    @Test
    void acceptWithoutDocumentCreatesCompanyWithNullCnpj() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn(new Email("dono@clinica.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(pendingInvitation()));
        CompanyResponse company = mock(CompanyResponse.class);
        when(company.id()).thenReturn(UUID.randomUUID().toString());
        when(companyUseCase.createCompany(any(CreateCompanyRequest.class), eq(userId))).thenReturn(company);
        CompanyDetails semDocumento = new CompanyDetails("Ana Souza", "Ana Estética", "  ",
                "ana@estetica.com", "11999990000", "01001000", "Rua A", "10", null,
                "Centro", "São Paulo", "SP");

        service.accept("tok", semDocumento, userId);

        ArgumentCaptor<CreateCompanyRequest> captor = ArgumentCaptor.forClass(CreateCompanyRequest.class);
        verify(companyUseCase).createCompany(captor.capture(), eq(userId));
        assertNull(captor.getValue().cnpj());
    }

    @Test
    void acceptRejectsAnotherEmail() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(new Email("outro@clinica.com"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(pendingInvitation()));

        assertThrows(InvitationRuleViolationException.class, () -> service.accept("tok", details(), userId));
        verify(companyUseCase, never()).createCompany(any(), any());
    }

    @Test
    void acceptRejectsRevokedInvitation() {
        CompanyInvitation revoked = pendingInvitation();
        revoked.revoke();
        when(repository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(revoked));

        assertThrows(InvitationNoLongerValidException.class,
                () -> service.accept("tok", details(), UUID.randomUUID()));
        verify(companyUseCase, never()).createCompany(any(), any());
    }
}
