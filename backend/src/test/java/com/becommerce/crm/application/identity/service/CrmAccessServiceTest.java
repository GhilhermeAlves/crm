package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.company.CompanyStatus;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Gate de acesso ao CRM (Sprint 6):
 *
 * <pre>
 *   users.is_active = true
 *   AND users.crm_enabled = true
 *   AND companies.status = ACTIVE
 * </pre>
 *
 * Qualquer falha → {@link CrmAccessDeniedException} (CRM_ACCESS_DENIED).
 */
@ExtendWith(MockitoExtension.class)
class CrmAccessServiceTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private CompanyRepository companyRepository;

    private CrmAccessService service;

    private Company activeCompany;
    private Company suspendedCompany;
    private Company inactiveCompany;

    @BeforeEach
    void setUp() {
        service = new CrmAccessService(companyRepository);
        activeCompany = company(CompanyStatus.ACTIVE);
        suspendedCompany = company(CompanyStatus.SUSPENDED);
        inactiveCompany = company(CompanyStatus.INACTIVE);
    }

    private Company company(CompanyStatus status) {
        Company created = Company.create(
                "Empresa LTDA", "Empresa", "12345678000190",
                "123456789", "987654321",
                "contato@empresa.com", "(11) 99999-0000", "https://empresa.com",
                "01001000", "Rua Teste", "100", "Sala 1",
                "Centro", "São Paulo", "SP", "Brasil",
                CompanyPlan.STARTER, 5, 1024, 500, null, null);
        return Company.reconstitute(
                created.getId(), created.getLegalName(), created.getTradingName(),
                created.getCnpj(), created.getStateRegistration(), created.getMunicipalRegistration(),
                created.getEmail(), created.getPhone(), created.getWebsite(),
                created.getAddressZipCode(), created.getAddressStreet(), created.getAddressNumber(),
                created.getAddressComplement(), created.getAddressNeighborhood(),
                created.getAddressCity(), created.getAddressState(), created.getAddressCountry(),
                created.getPlan(), status,
                created.getMaxUsers(), created.getMaxStorageMb(), created.getMaxContacts(),
                created.getLogoUrl(), created.getNotes(),
                created.getCreatedAt(), created.getUpdatedAt());
    }

    private User user(boolean active, boolean crmEnabled) {
        User user = User.create(new Email("ghilherme007@gmail.com"),
                new Password("Kc!Valid1Aa1"), "Ghilherme", "Santos", COMPANY_ID);
        user.setActive(active);
        user.setCrmEnabled(crmEnabled);
        return user;
    }

    // ------------------------------------------------------------------ ALLOW

    @Test
    void shouldAllowActiveUserWithCrmAccessAndActiveCompany() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(activeCompany));

        assertTrue(service.hasCrmAccess(user(true, true)));
        service.assertCrmAccess(user(true, true));
    }

    // ------------------------------------------------------------------ DENY

    @Test
    void shouldDenyInactiveUserEvenWithCrmAccessAndActiveCompany() {
        assertThrows(CrmAccessDeniedException.class,
                () -> service.assertCrmAccess(user(false, true)));
        assertFalse(service.hasCrmAccess(user(false, true)));
    }

    @Test
    void shouldDenyUserWithoutCrmAccessEvenWhenActiveAndCompanyActive() {
        assertThrows(CrmAccessDeniedException.class,
                () -> service.assertCrmAccess(user(true, false)));
        assertFalse(service.hasCrmAccess(user(true, false)));
    }

    @Test
    void shouldDenyActiveUserWithCrmAccessWhenCompanySuspended() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(suspendedCompany));

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class,
                () -> service.assertCrmAccess(user(true, true)));

        assertTrue(ex.getMessage().contains("SUSPENDED"));
        assertFalse(service.hasCrmAccess(user(true, true)));
    }

    @Test
    void shouldDenyActiveUserWithCrmAccessWhenCompanyInactive() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(inactiveCompany));

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class,
                () -> service.assertCrmAccess(user(true, true)));

        assertTrue(ex.getMessage().contains("INACTIVE"));
        assertFalse(service.hasCrmAccess(user(true, true)));
    }

    @Test
    void shouldDenyWhenUserDoesNotExist() {
        assertThrows(CrmAccessDeniedException.class, () -> service.assertCrmAccess(null));
        assertFalse(service.hasCrmAccess(null));
    }

    @Test
    void shouldDenyWhenCompanyDoesNotExist() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        CrmAccessDeniedException ex = assertThrows(CrmAccessDeniedException.class,
                () -> service.assertCrmAccess(user(true, true)));

        assertTrue(ex.getMessage().contains("Empresa não encontrada"));
        assertFalse(service.hasCrmAccess(user(true, true)));
    }

    @Test
    void shouldDenyWhenCompanyIdIsInvalid() {
        when(companyRepository.findById(any())).thenReturn(Optional.empty());
        User user = user(true, true);
        user.setCompanyId(UUID.randomUUID());

        assertThrows(CrmAccessDeniedException.class, () -> service.assertCrmAccess(user));
    }

    @Test
    void shouldDenyWhenUserHasNoCompany() {
        when(companyRepository.findById(any())).thenReturn(Optional.empty());
        User user = user(true, true);
        user.setCompanyId(null);

        assertThrows(CrmAccessDeniedException.class, () -> service.assertCrmAccess(user));
    }
}
