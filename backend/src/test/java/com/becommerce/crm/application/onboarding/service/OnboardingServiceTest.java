package com.becommerce.crm.application.onboarding.service;

import com.becommerce.crm.application.company.dto.CompanyResponse;
import com.becommerce.crm.application.company.dto.CreateCompanyRequest;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.infrastructure.identity.persistence.RoleSeedService;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");

    @Mock private CompanyRepository companyRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleSeedService roleSeedService;
    @Mock private CompanyUseCase companyUseCase;
    @Mock private com.becommerce.crm.application.workflow.service.WorkflowTemplateSeeder workflowTemplateSeeder;

    private OnboardingService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingService(companyRepository, membershipRepository, roleRepository,
                userRoleRepository, userRepository, roleSeedService, companyUseCase, workflowTemplateSeeder);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private User owner() {
        User user = User.create(new Email("owner@empresa.com"), null, "Owner", "Dono", null);
        user.setId(USER_ID);
        return user;
    }

    private CreateCompanyRequest request() {
        return new CreateCompanyRequest(
                "Minha Empresa LTDA", "Minha", "12345678000190",
                null, null, "contato@minha.com", "(11) 99999-0000",
                null, "01001000", "Rua Teste", "100", null,
                "Centro", "São Paulo", "SP", "Brasil",
                "STARTER", null, null, null, null, null);
    }

    private CompanyResponse response(UUID companyId) {
        return new CompanyResponse(
                companyId.toString(), "Minha Empresa LTDA", "Minha", "12345678000190",
                null, null, "contato@minha.com", "(11) 99999-0000", null,
                null, "active", "starter", 5, 1024, 500, null, null,
                null, null);
    }

    /**
     * Configura o pipeline feliz: permite criação, captura a empresa realmente
     * criada pelo serviço (UUID próprio via {@code Company.create}) e devolve a
     * empresa com o mesmo id, além de conceder look-up imediato pelo company id.
     */
    private AtomicReference<UUID> arrangeHappyPath() {
        when(companyRepository.existsByCnpj(any())).thenReturn(false);
        when(companyRepository.existsByEmail(any())).thenReturn(false);

        AtomicReference<UUID> createdId = new AtomicReference<>();
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            createdId.set(c.getId());
            return c;
        });
        when(membershipRepository.existsActiveByUserIdAndCompanyId(eq(USER_ID), any())).thenReturn(false);
        when(roleRepository.findByNameAndCompanyId(any(), any())).thenAnswer(inv ->
                Optional.of(Role.createSystem(RoleName.ADMIN.name(), inv.getArgument(1))));
        when(userRoleRepository.existsByUserIdAndRoleId(any(), any())).thenReturn(false);
        when(companyUseCase.getCompanyById(any(), any(), anyBoolean())).thenAnswer(inv ->
                response(inv.getArgument(0)));
        return createdId;
    }

    @Test
    void shouldOnboardCompanyForOwnerWithoutCompany() {
        AtomicReference<UUID> createdId = arrangeHappyPath();

        CompanyResponse result = service.onboard(request(), owner());

        assertNotNull(result);
        assertEquals("Minha Empresa LTDA", result.legalName());

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertEquals(USER_ID, membershipCaptor.getValue().getUserId());
        assertEquals(createdId.get(), membershipCaptor.getValue().getCompanyId());
        assertEquals(Membership.OWNER_ROLE, membershipCaptor.getValue().getRole());

        verify(roleSeedService).seedRoles(createdId.get());

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertEquals(USER_ID, roleCaptor.getValue().getUserId());
        assertEquals(createdId.get(), roleCaptor.getValue().getCompanyId());
    }

    @Test
    void shouldGrantCrmAccessAndElevateCompanyIdOnOwner() {
        AtomicReference<UUID> createdId = arrangeHappyPath();

        User owner = owner();
        service.onboard(request(), owner);

        assertTrue(owner.isCrmEnabled());
        assertEquals(createdId.get(), owner.getCompanyId());
        verify(userRepository).save(owner);
    }

    @Test
    void shouldSetTenantContextBeforeSeedingRoles() {
        AtomicReference<UUID> createdId = arrangeHappyPath();
        final boolean[] tenantWasSet = {false};
        doAnswer(inv -> {
            tenantWasSet[0] = TenantContext.hasCompanyId() && createdId.get().equals(TenantContext.getCompanyId());
            return null;
        }).when(roleSeedService).seedRoles(any());

        service.onboard(request(), owner());

        assertTrue(tenantWasSet[0]);
    }

    @Test
    void shouldRejectWhenCnpjAlreadyExists() {
        when(companyRepository.existsByCnpj(any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.onboard(request(), owner()));
        verify(companyRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenEmailAlreadyExists() {
        when(companyRepository.existsByCnpj(any())).thenReturn(false);
        when(companyRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.onboard(request(), owner()));
        verify(companyRepository, never()).save(any());
    }
}