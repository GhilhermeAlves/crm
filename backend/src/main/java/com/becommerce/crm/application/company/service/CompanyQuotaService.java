package com.becommerce.crm.application.company.service;

import com.becommerce.crm.application.company.dto.CompanyUsageResponse;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.invitation.port.output.InvitationRepository;
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.application.storage.port.output.StorageRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyNotFoundException;
import com.becommerce.crm.domain.invitation.InvitationStatus;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Autoridade central de quota SaaS (Sprint 8.6). Calcula o uso corrente por
 * empresa (usuários ativos, contatos ativos, bytes de armazenamento) e lê os
 * limites do plano na {@link Company}. Todas as contagens rodam sob o contexto
 * de tenant correspondente (RLS FORCE), preservando o contexto atual.
 */
@Service
public class CompanyQuotaService {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final CompanyRepository companyRepository;
    private final MembershipRepository membershipRepository;
    private final InvitationRepository invitationRepository;
    private final ContactRepository contactRepository;
    private final StorageRepository storageRepository;

    public CompanyQuotaService(CompanyRepository companyRepository,
                               MembershipRepository membershipRepository,
                               InvitationRepository invitationRepository,
                               ContactRepository contactRepository,
                               StorageRepository storageRepository) {
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.contactRepository = contactRepository;
        this.storageRepository = storageRepository;
    }

    public Company requireCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    public long countActiveUsers(UUID companyId) {
        return withTenant(companyId, () -> membershipRepository.countActiveByCompanyId(companyId));
    }

    public long countPendingInvitations(UUID companyId) {
        return withTenant(companyId,
                () -> invitationRepository.findByCompanyId(companyId, InvitationStatus.PENDING).size());
    }

    public long countContacts(UUID companyId) {
        return withTenant(companyId, () -> contactRepository.countActiveByCompanyId(companyId));
    }

    public long storageBytes(UUID companyId) {
        return withTenant(companyId, () -> storageRepository.sumSizeByCompanyId(companyId));
    }

    public CompanyUsageResponse usage(UUID companyId) {
        Company company = requireCompany(companyId);
        return new CompanyUsageResponse(
                new CompanyUsageResponse.UsageItem(
                        Math.toIntExact(countActiveUsers(companyId)), company.getMaxUsers()),
                new CompanyUsageResponse.UsageItem(
                        Math.toIntExact(countContacts(companyId)), company.getMaxContacts()),
                new CompanyUsageResponse.StorageUsage(
                        storageBytes(companyId) / BYTES_PER_MB, company.getMaxStorageMb()));
    }

    /** Assere que adicionar {@code additionalBytes} não estoura max_storage_mb. */
    public void assertCanAddSpace(UUID companyId, long additionalBytes) {
        Company company = requireCompany(companyId);
        long limitBytes = (long) company.getMaxStorageMb() * BYTES_PER_MB;
        long current = storageBytes(companyId);
        if (current + additionalBytes > limitBytes) {
            throw new com.becommerce.crm.domain.quota.exception.QuotaExceededException(
                    "Limite de armazenamento da empresa atingido (" + company.getMaxStorageMb() + " MB).");
        }
    }

    /** Assere que criar mais um contato não estoura max_contacts. */
    public void assertCanAddContact(UUID companyId) {
        Company company = requireCompany(companyId);
        if (countContacts(companyId) >= company.getMaxContacts()) {
            throw new com.becommerce.crm.domain.quota.exception.QuotaExceededException(
                    "Limite de contatos da empresa atingido (" + company.getMaxContacts() + ").");
        }
    }

    private long withTenant(UUID companyId, LongSupplier supplier) {
        UUID previous = TenantContext.getCompanyId();
        boolean setOs = previous == null || !previous.equals(companyId);
        try {
            if (setOs) {
                TenantContext.setCompanyId(companyId);
            }
            return supplier.getAsLong();
        } finally {
            if (setOs) {
                if (previous == null) {
                    TenantContext.clear();
                } else {
                    TenantContext.setCompanyId(previous);
                }
            }
        }
    }
}