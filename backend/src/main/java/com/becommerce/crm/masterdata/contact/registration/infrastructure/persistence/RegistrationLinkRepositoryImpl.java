package com.becommerce.crm.masterdata.contact.registration.infrastructure.persistence;

import com.becommerce.crm.masterdata.contact.registration.application.port.RegistrationLinkRepository;
import com.becommerce.crm.masterdata.contact.registration.domain.RegistrationLink;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RegistrationLinkRepositoryImpl implements RegistrationLinkRepository {

    private final RegistrationLinkJpaRepository jpa;

    public RegistrationLinkRepositoryImpl(RegistrationLinkJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RegistrationLink save(RegistrationLink link) {
        return toDomain(jpa.save(new JpaRegistrationLink(link.getId(), link.getCompanyId(),
                link.getContactId(), link.getTokenHash(), link.getExpiresAt(), link.getUsedAt(),
                link.getCreatedBy(), link.getCreatedAt())));
    }

    @Override
    public Optional<RegistrationLink> findByTokenHashForUpdate(String tokenHash) {
        return jpa.findByTokenForUpdate(tokenHash).map(RegistrationLinkRepositoryImpl::toDomain);
    }

    @Override
    public Optional<RegistrationLink> findByTokenHash(String tokenHash) {
        return jpa.findByToken(tokenHash).map(RegistrationLinkRepositoryImpl::toDomain);
    }

    @Override
    public int invalidateUnused(UUID companyId, UUID contactId, LocalDateTime now) {
        return jpa.invalidateUnused(companyId, contactId, now);
    }

    @Override
    public void markUsed(UUID linkId, LocalDateTime now) {
        jpa.markUsed(linkId, now);
    }

    private static RegistrationLink toDomain(JpaRegistrationLink e) {
        return new RegistrationLink(e.getId(), e.getCompanyId(), e.getContactId(), e.getToken(),
                e.getExpiresAt(), e.getUsedAt(), e.getCreatedBy(), e.getCreatedAt());
    }
}
