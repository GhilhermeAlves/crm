package com.becommerce.crm.application.company.service;

import com.becommerce.crm.application.company.dto.CompanyUsageResponse;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.invitation.port.output.InvitationRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.application.storage.port.output.StorageRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.quota.exception.QuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CompanyQuotaServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock InvitationRepository invitationRepository;
    @Mock ContactRepository contactRepository;
    @Mock StorageRepository storageRepository;

    private CompanyQuotaService quotaService;

    private final UUID companyA = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final UUID companyB = UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444");

    @BeforeEach
    void setUp() {
        quotaService = new CompanyQuotaService(companyRepository, membershipRepository,
                invitationRepository, contactRepository, storageRepository);
    }

    private Company company(int maxUsers, int maxContacts, int maxStorageMb) {
        return Company.create("Empresa", "Empresa", "12345678000190",
                "1", "1", "e@e.com", "1", null, "0100", "Rua", "1", null,
                "Centro", "Cid", "SP", "BR", CompanyPlan.PROFESSIONAL,
                maxUsers, maxStorageMb, maxContacts, null, null);
    }

    @Test
    void shouldAllowContactBelowLimit() {
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(500, 10, 100)));
        when(contactRepository.countActiveByCompanyId(companyA)).thenReturn(9L);
        assertDoesNotThrow(() -> quotaService.assertCanAddContact(companyA));
    }

    @Test
    void shouldBlockContactAtLimit() {
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(500, 10, 100)));
        when(contactRepository.countActiveByCompanyId(companyA)).thenReturn(10L);
        assertThrows(QuotaExceededException.class, () -> quotaService.assertCanAddContact(companyA));
        verify(contactRepository).countActiveByCompanyId(companyA);
    }

    @Test
    void shouldNotAllowOtherCompanyToInfluenceContacts() {
        // companyA no limite -> bloqueado, independente de outras empresas.
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(500, 10, 100)));
        when(contactRepository.countActiveByCompanyId(companyA)).thenReturn(10L);
        assertThrows(QuotaExceededException.class, () -> quotaService.assertCanAddContact(companyA));
    }

    @Test
    void shouldAllowUploadWithinStorageQuota() {
        // maxStorageMb = 1 -> 1 MB; current 0; add 512 KB = permitido.
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(5, 500, 1)));
        when(storageRepository.sumSizeByCompanyId(companyA)).thenReturn(0L);
        assertDoesNotThrow(() -> quotaService.assertCanAddSpace(companyA, 512 * 1024));
    }

    @Test
    void shouldBlockUploadOverStorageQuota() {
        // maxStorageMb = 1; current 512 KB; add 512 KB + 1 byte -> estoura.
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(5, 500, 1)));
        when(storageRepository.sumSizeByCompanyId(companyA)).thenReturn(512L * 1024L);
        assertThrows(QuotaExceededException.class, () -> quotaService.assertCanAddSpace(companyA, 512L * 1024L + 1));
    }

    @Test
    void shouldNotAffectAnotherCompanyStorageQuota() {
        // Empresa A no limite (conservadora): outra empresa não altera o cálculo.
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(5, 500, 1)));
        when(storageRepository.sumSizeByCompanyId(companyA)).thenReturn(2L * 1024L * 1024L);
        assertThrows(QuotaExceededException.class, () -> quotaService.assertCanAddSpace(companyA, 1024L));
    }

    @Test
    void shouldComputeUsageWithCorrectValuesAndLimits() {
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(10, 1000, 5000)));
        when(membershipRepository.countActiveByCompanyId(companyA)).thenReturn(4L);
        when(contactRepository.countActiveByCompanyId(companyA)).thenReturn(120L);
        // 350 MB em bytes
        when(storageRepository.sumSizeByCompanyId(companyA)).thenReturn(350L * 1024L * 1024L);

        CompanyUsageResponse usage = quotaService.usage(companyA);

        assertEquals(4, usage.users().current());
        assertEquals(10, usage.users().limit());
        assertEquals(120, usage.contacts().current());
        assertEquals(1000, usage.contacts().limit());
        assertEquals(350L, usage.storage().currentMb());
        assertEquals(5000, usage.storage().limitMb());
    }

    @Test
    void shouldIsolateUsageByCompany() {
        when(companyRepository.findById(companyA)).thenReturn(Optional.of(company(5, 500, 100)));
        when(membershipRepository.countActiveByCompanyId(companyA)).thenReturn(2L);
        when(contactRepository.countActiveByCompanyId(companyA)).thenReturn(10L);
        when(storageRepository.sumSizeByCompanyId(companyA)).thenReturn(0L);

        CompanyUsageResponse usageA = quotaService.usage(companyA);
        assertEquals(5, usageA.users().limit());
        assertEquals(500, usageA.contacts().limit());

        // companyB não é lido para o usage de companyA.
        verify(contactRepository).countActiveByCompanyId(companyA);
        verifyNoMoreInteractions(contactRepository, membershipRepository, storageRepository);
    }

    @Test
    void shouldCountPendingInvitationsTowardUsers() {
        when(invitationRepository.findByCompanyId(any(), any())).thenReturn(List.of());
        assertEquals(0L, quotaService.countPendingInvitations(companyA));
    }
}