package com.becommerce.crm.infrastructure.omnichannel.persistence;

import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelProvider;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.ChannelType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OmnichannelChannelRepositoryImpl implements OmnichannelChannelRepository {

    private final OmnichannelChannelJpaRepository jpaRepository;

    public OmnichannelChannelRepositoryImpl(OmnichannelChannelJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Channel save(Channel channel) {
        return toDomain(jpaRepository.save(toEntity(channel)));
    }

    @Override
    public Optional<Channel> findById(UUID id) {
        return jpaRepository.findById(id).map(OmnichannelChannelRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Channel> findByCompanyAndExternalId(UUID companyId, String externalId) {
        return jpaRepository.findByCompanyIdAndExternalId(companyId, externalId)
                .map(OmnichannelChannelRepositoryImpl::toDomain);
    }

    @Override
    public List<Channel> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(OmnichannelChannelRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public void delete(Channel channel) {
        jpaRepository.delete(toEntity(channel));
    }

    private static OmnichannelChannelJpaEntity toEntity(Channel c) {
        OmnichannelChannelJpaEntity e = new OmnichannelChannelJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setType(c.getType().name());
        e.setProvider(c.getProvider().name());
        e.setName(c.getName());
        e.setStatus(c.getStatus().name());
        e.setExternalId(c.getExternalId());
        e.setConfig(c.getConfig());
        e.setSecretsRef(c.getSecretsRef());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    private static Channel toDomain(OmnichannelChannelJpaEntity e) {
        return Channel.reconstitute(e.getId(), e.getCompanyId(),
                ChannelType.valueOf(e.getType()), ChannelProvider.valueOf(e.getProvider()),
                e.getName(), ChannelStatus.valueOf(e.getStatus()), e.getExternalId(),
                e.getConfig(), e.getSecretsRef(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
