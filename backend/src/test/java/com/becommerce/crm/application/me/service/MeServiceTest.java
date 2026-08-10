package com.becommerce.crm.application.me.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.me.dto.CompanyOptionResponse;
import com.becommerce.crm.application.me.port.output.MyCompanyProjection;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.membership.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    private static final UUID USER_ID = UUID.fromString("974bbedb-298d-4ec6-a037-514b24c248e4");
    private static final UUID COMPANY_A = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID COMPANY_B = UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444");
    private static final UUID COMPANY_X = UUID.fromString("cccccccc-1111-2222-3333-444444444444");

    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;

    private MeService service;

    @BeforeEach
    void setUp() {
        service = new MeService(membershipRepository, userRepository, companyRepository);
    }

    private User userWithCompany(UUID companyId) {
        User user = new User();
        user.setId(USER_ID);
        user.setCompanyId(companyId);
        return user;
    }

    private MyCompanyProjection option(UUID companyId, String name, String logo) {
        return new MyCompanyProjection() {
            @Override public UUID getCompanyId() { return companyId; }
            @Override public String getCompanyName() { return name; }
            @Override public String getLogoUrl() { return logo; }
            @Override public String getRole() { return "AGENT"; }
        };
    }

    private Company company(UUID id, String tradingName) {
        return Company.create(
                tradingName, tradingName, "12345678000190",
                "1", "1", "c@e.com", "11 99999", null,
                "0100", "Rua", "1", null, "Centro", "Cid", "SP", "BR",
                CompanyPlan.STARTER, 5, 1024, 500, null, null);
    }

    @Test
    void shouldListOnlyCompaniesWithActiveMembershipMarkingTheActiveOne() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithCompany(COMPANY_A)));
        when(membershipRepository.findActiveCompanyOptionsByUserId(USER_ID))
                .thenReturn(List.of(option(COMPANY_A, "Empresa A", "logo-a.png"),
                                    option(COMPANY_B, "Empresa B", null)));

        List<CompanyOptionResponse> result = service.listMyCompanies(USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.get(0).active(), "empresa ativa (users.company_id) marcada como active");
        assertFalse(result.get(1).active(), "empresa inativa marcada como active=false");
        assertEquals("logo-a.png", result.get(0).logo());
    }

    @Test
    void shouldReturnEmptyListForCompanyLessUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithCompany(null)));
        when(membershipRepository.findActiveCompanyOptionsByUserId(USER_ID)).thenReturn(List.of());

        List<CompanyOptionResponse> result = service.listMyCompanies(USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSwitchActiveCompanyWhenMembershipIsActive() {
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_B)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithCompany(COMPANY_A)));
        Company companyB = company(COMPANY_B, "Empresa B");
        // Company.create gera um id próprio; forçamos o id esperado para a resolução.
        when(companyRepository.findById(COMPANY_B)).thenReturn(Optional.of(companyB));

        CompanyOptionResponse result = service.switchCompany(USER_ID, COMPANY_B);

        assertEquals(COMPANY_B, result.companyId());
        assertTrue(result.active());
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                u -> COMPANY_B.equals(u.getCompanyId())));
    }

    @Test
    void shouldRejectSwitchWithoutActiveMembership() {
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_B)).thenReturn(false);

        assertThrows(MembershipNotFoundException.class, () -> service.switchCompany(USER_ID, COMPANY_B));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectSwitchToInexistentCompany() {
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_X)).thenReturn(false);

        assertThrows(MembershipNotFoundException.class, () -> service.switchCompany(USER_ID, COMPANY_X));
    }

    @Test
    void shouldBeIdempotentWhenSwitchingToCurrentCompany() {
        when(membershipRepository.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_A)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithCompany(COMPANY_A)));
        Company companyA = company(COMPANY_A, "Empresa A");
        when(companyRepository.findById(COMPANY_A)).thenReturn(Optional.of(companyA));

        CompanyOptionResponse result = service.switchCompany(USER_ID, COMPANY_A);

        assertEquals(COMPANY_A, result.companyId());
        assertTrue(result.active());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}