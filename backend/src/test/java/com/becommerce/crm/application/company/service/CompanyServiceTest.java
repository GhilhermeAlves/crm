package com.becommerce.crm.application.company.service;

import com.becommerce.crm.application.company.dto.*;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.company.port.output.CompanySettingsRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.*;
import com.becommerce.crm.domain.company.event.CompanyCreatedEvent;
import com.becommerce.crm.domain.company.event.CompanyDeletedEvent;
import com.becommerce.crm.domain.company.event.CompanyUpdatedEvent;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.identity.persistence.RoleSeedService;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySettingsRepository companySettingsRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private RoleSeedService roleSeedService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private CompanyService companyService;

    private Company sampleCompany;
    private User sampleUser;
    private Role sampleAdminRole;

    @BeforeEach
    void setUp() {
        sampleCompany = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, 500, null, null
        );
        sampleUser = mock(User.class);
        lenient().when(sampleUser.getId()).thenReturn(UUID.randomUUID());
        sampleAdminRole = mock(Role.class);
        lenient().when(sampleAdminRole.getId()).thenReturn(UUID.randomUUID());
        lenient().when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(sampleUser));
        lenient().when(roleRepository.findByNameAndCompanyId(anyString(), any(UUID.class)))
                .thenReturn(Optional.of(sampleAdminRole));
        lenient().when(userRoleRepository.existsByUserIdAndRoleId(any(UUID.class), any(UUID.class)))
                .thenReturn(false);
    }

    @Test
    void shouldGetOwnCompany() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));

        CompanyResponse response = companyService.getCompanyById(
                sampleCompany.getId(), sampleCompany.getId(), false);

        assertEquals(sampleCompany.getId().toString(), response.id());
        assertEquals("Empresa LTDA", response.legalName());
        assertEquals("12345678000190", response.cnpj());
        assertEquals("active", response.status());
        assertEquals("starter", response.plan());
        assertEquals(500, response.maxContacts());
    }

    @Test
    void shouldGetAnyCompanyAsSuperAdmin() {
        UUID otherCompanyId = UUID.randomUUID();
        when(companyRepository.findById(otherCompanyId)).thenReturn(Optional.of(sampleCompany));

        CompanyResponse response = companyService.getCompanyById(otherCompanyId, sampleCompany.getId(), true);

        assertNotNull(response);
        verify(companyRepository).findById(otherCompanyId);
    }

    @Test
    void shouldThrowWhenAccessingOtherCompanyAsMember() {
        UUID otherCompanyId = UUID.randomUUID();
        when(companyRepository.findById(otherCompanyId)).thenReturn(Optional.of(sampleCompany));

        assertThrows(CrmAccessDeniedException.class,
                () -> companyService.getCompanyById(otherCompanyId, sampleCompany.getId(), false));
    }

    @Test
    void shouldThrowWhenCompanyNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class,
                () -> companyService.getCompanyById(id, id, true));
    }

    @Test
    void shouldListAllCompaniesForSuperAdmin() {
        Company another = Company.create(
                "Outra LTDA", "Outra", "99999999000199",
                null, null,
                "contato@outra.com", "(11) 88888-0000", null,
                "02002000", "Rua Outra", "200", null,
                "Bairro", "Rio de Janeiro", "RJ", "Brasil",
                CompanyPlan.PROFESSIONAL, 10, 2048, 1000, null, null
        );

        when(companyRepository.findAll()).thenReturn(List.of(sampleCompany, another));

        List<CompanySummaryResponse> result = companyService.listCompanies(sampleCompany.getId(), true);

        assertEquals(2, result.size());
        assertEquals("Empresa LTDA", result.get(0).legalName());
        assertEquals("Outra LTDA", result.get(1).legalName());
    }

    @Test
    void shouldListOnlyOwnCompanyForMember() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));

        List<CompanySummaryResponse> result = companyService.listCompanies(sampleCompany.getId(), false);

        assertEquals(1, result.size());
        assertEquals("Empresa LTDA", result.get(0).legalName());
        verify(companyRepository, never()).findAll();
    }

    @Test
    void shouldCreateCompanyWithDefaultMaxContacts() {
        when(companyRepository.existsByCnpj("12345678000190")).thenReturn(false);
        when(companyRepository.existsByEmail("contato@empresa.com")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(sampleCompany);
        when(membershipRepository.existsActiveByUserIdAndCompanyId(any(UUID.class), any(UUID.class)))
                .thenReturn(false);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", 5, 1024, null, null, null
        );

        CompanyResponse response = companyService.createCompany(request, sampleUser.getId());

        assertNotNull(response.id());
        assertEquals("Empresa LTDA", response.legalName());

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertEquals(CompanyPlan.STARTER, captor.getValue().getPlan());
        assertEquals(CompanyStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(500, captor.getValue().getMaxContacts());

        verify(roleSeedService).seedRoles(any(UUID.class));
        verify(membershipRepository).save(any());
        verify(userRoleRepository).save(any());
        verify(eventPublisher).publish(any(CompanyCreatedEvent.class));
    }

    @Test
    void shouldThrowWhenCnpjAlreadyExists() {
        when(companyRepository.existsByCnpj("12345678000190")).thenReturn(true);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa LTDA", "Empresa", "12345678000190",
                null, null,
                "contato@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", null, null, null, null, null
        );

        assertThrows(CompanyAlreadyExistsException.class, () -> companyService.createCompany(request, sampleUser.getId()));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(companyRepository.existsByCnpj("12345678000190")).thenReturn(false);
        when(companyRepository.existsByEmail("contato@empresa.com")).thenReturn(true);

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa LTDA", "Empresa", "12345678000190",
                null, null,
                "contato@empresa.com", "(11) 99999-0000", null,
                "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", null, null, null, null, null
        );

        assertThrows(CompanyAlreadyExistsException.class, () -> companyService.createCompany(request, sampleUser.getId()));
    }

    @Test
    void shouldUpdateOwnCompany() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class))).thenReturn(sampleCompany);

        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Updated LTDA", null, null, null, null,
                null, null, null, null, null, null, null, null,
                "PROFESSIONAL", null, 10, null, 1000, null, null
        );

        CompanyResponse response = companyService.updateCompany(
                sampleCompany.getId(), request, sampleCompany.getId(), false);

        assertEquals("Updated LTDA", response.legalName());
        verify(eventPublisher).publish(any(CompanyUpdatedEvent.class));
    }

    @Test
    void shouldThrowWhenUpdatingOtherCompanyAsMember() {
        UUID otherCompanyId = UUID.randomUUID();
        when(companyRepository.findById(otherCompanyId)).thenReturn(Optional.of(sampleCompany));

        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Updated LTDA", null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null
        );

        assertThrows(CrmAccessDeniedException.class,
                () -> companyService.updateCompany(otherCompanyId, request, sampleCompany.getId(), false));
    }

    @Test
    void shouldPreventDeletingOwnCompany() {
        when(companyRepository.findById(sampleCompany.getId())).thenReturn(Optional.of(sampleCompany));

        assertThrows(CompanyDeletionForbiddenException.class,
                () -> companyService.deleteCompany(sampleCompany.getId(), sampleCompany.getId(), true));
        verify(companyRepository, never()).deleteById(any());
        verify(eventPublisher, never()).publish(any(CompanyDeletedEvent.class));
    }

    @Test
    void shouldAllowSuperAdminDeletingOtherCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        when(companyRepository.findById(otherCompanyId)).thenReturn(Optional.of(sampleCompany));
        doNothing().when(companyRepository).deleteById(otherCompanyId);

        companyService.deleteCompany(otherCompanyId, sampleCompany.getId(), true);

        verify(companyRepository).deleteById(otherCompanyId);
        verify(eventPublisher).publish(any(CompanyDeletedEvent.class));
    }

    @Test
    void shouldThrowWhenDeletingNonexistentCompany() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class,
                () -> companyService.deleteCompany(id, id, true));
        verify(companyRepository, never()).deleteById(any());
    }

    @Test
    void shouldGetSettingsWithDefaultsWhenAbsent() {
        UUID companyId = UUID.randomUUID();
        when(companySettingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        CompanySettingsResponse response = companyService.getCompanySettings(companyId, companyId);

        assertEquals(companyId.toString(), response.companyId());
        assertEquals("America/Sao_Paulo", response.timezone());
        assertEquals("pt-BR", response.locale());
        assertEquals("BRL", response.currency());
    }

    @Test
    void shouldGetPersistedSettings() {
        UUID companyId = UUID.randomUUID();
        CompanySettings settings = CompanySettings.create(
                companyId, "America/New_York", "en-US", "USD", "{\"seg\":\"09-18\"}", "{\"email\":true}");
        when(companySettingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(settings));

        CompanySettingsResponse response = companyService.getCompanySettings(companyId, companyId);

        assertEquals("America/New_York", response.timezone());
        assertEquals("en-US", response.locale());
        assertEquals("USD", response.currency());
        assertEquals("{\"seg\":\"09-18\"}", response.businessHours());
    }

    @Test
    void shouldThrowWhenGettingSettingsOfOtherCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        UUID requesterCompanyId = UUID.randomUUID();

        assertThrows(CrmAccessDeniedException.class,
                () -> companyService.getCompanySettings(otherCompanyId, requesterCompanyId));
    }

    @Test
    void shouldUpdateSettingsCreatingWhenAbsent() {
        UUID companyId = UUID.randomUUID();
        when(companySettingsRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());
        when(companySettingsRepository.save(any(CompanySettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCompanySettingsRequest request = new UpdateCompanySettingsRequest(
                "America/Sao_Paulo", "pt-BR", "BRL", "{\"seg\":\"09-18\"}", null);

        CompanySettingsResponse response = companyService.updateCompanySettings(companyId, request, companyId);

        assertEquals("America/Sao_Paulo", response.timezone());
        assertEquals("{\"seg\":\"09-18\"}", response.businessHours());
        ArgumentCaptor<CompanySettings> captor = ArgumentCaptor.forClass(CompanySettings.class);
        verify(companySettingsRepository).save(captor.capture());
        assertEquals(companyId, captor.getValue().getCompanyId());
    }

    @Test
    void shouldUpdateExistingSettingsPartially() {
        UUID companyId = UUID.randomUUID();
        CompanySettings existing = CompanySettings.create(
                companyId, "America/Sao_Paulo", "pt-BR", "BRL", "{\"seg\":\"09-18\"}", null);
        when(companySettingsRepository.findByCompanyId(companyId)).thenReturn(Optional.of(existing));
        when(companySettingsRepository.save(any(CompanySettings.class))).thenReturn(existing);

        UpdateCompanySettingsRequest request = new UpdateCompanySettingsRequest("Europe/Lisbon", null, null, null, null);

        CompanySettingsResponse response = companyService.updateCompanySettings(companyId, request, companyId);

        assertEquals("Europe/Lisbon", response.timezone());
        assertEquals("pt-BR", response.locale());
        assertEquals("BRL", response.currency());
    }
}
