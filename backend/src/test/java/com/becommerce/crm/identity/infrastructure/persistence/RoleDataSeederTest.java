package com.becommerce.crm.identity.infrastructure.persistence;

import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyStatus;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleDataSeederTest {

    @Mock
    private RoleSeedService roleSeedService;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private RoleDataSeeder seeder;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSeedOnlyActiveCompaniesReadingEachUnderItsOwnTenant() {
        UUID active = UUID.randomUUID();
        UUID suspended = UUID.randomUUID();
        Company activeCompany = company(CompanyStatus.ACTIVE);
        Company suspendedCompany = company(CompanyStatus.SUSPENDED);
        when(companyRepository.findAllIds()).thenReturn(List.of(active, suspended));
        AtomicReference<UUID> tenantOnRead = new AtomicReference<>();
        when(companyRepository.findById(active)).thenAnswer(inv -> {
            tenantOnRead.set(TenantContext.getCompanyId());
            return Optional.of(activeCompany);
        });
        when(companyRepository.findById(suspended)).thenReturn(Optional.of(suspendedCompany));
        AtomicReference<UUID> tenantOnSeed = new AtomicReference<>();
        doAnswer(inv -> {
            tenantOnSeed.set(TenantContext.getCompanyId());
            return null;
        }).when(roleSeedService).seedRoles(active);

        seeder.run();

        verify(roleSeedService).seedRoles(active);
        verify(roleSeedService, never()).seedRoles(suspended);
        assertThat(tenantOnRead.get()).isEqualTo(active);
        assertThat(tenantOnSeed.get()).isEqualTo(active);
        assertThat(TenantContext.hasCompanyId()).isFalse();
    }

    @Test
    void shouldSkipWhenThereAreNoCompanies() {
        when(companyRepository.findAllIds()).thenReturn(List.of());

        seeder.run();

        verify(roleSeedService, never()).seedRoles(any());
    }

    private static Company company(CompanyStatus status) {
        Company company = mock(Company.class);
        when(company.getStatus()).thenReturn(status);
        return company;
    }
}
