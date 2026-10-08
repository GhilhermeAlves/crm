package com.becommerce.crm.communication.omnichannel.infrastructure.persistence;

import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OmnichannelIgnoredContactRepositoryImpl implements OmnichannelIgnoredContactRepository {

    private final OmnichannelIgnoredContactJpaRepository jpaRepository;

    public OmnichannelIgnoredContactRepositoryImpl(OmnichannelIgnoredContactJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<IgnoredContact> findByCompany(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(OmnichannelIgnoredContactRepositoryImpl::toDomain).toList();
    }

    @Override
    public boolean existsByCompanyAndPhone(UUID companyId, String phone) {
        return jpaRepository.existsByCompanyIdAndPhone(companyId, phone);
    }

    @Override
    public IgnoredContact save(IgnoredContact contact) {
        OmnichannelIgnoredContactJpaEntity e = new OmnichannelIgnoredContactJpaEntity();
        e.setId(contact.id());
        e.setCompanyId(contact.companyId());
        e.setPhone(contact.phone());
        e.setLabel(contact.label());
        e.setCreatedAt(contact.createdAt());
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public void delete(UUID companyId, UUID id) {
        jpaRepository.deleteByCompanyIdAndId(companyId, id);
    }

    private static IgnoredContact toDomain(OmnichannelIgnoredContactJpaEntity e) {
        return new IgnoredContact(e.getId(), e.getCompanyId(), e.getPhone(), e.getLabel(), e.getCreatedAt());
    }
}
